package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeAttempt
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeAttemptJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.Optional

class BillingChargeExecutorTest {
    private val subscriptionRepository = mockk<TossBillingSubscriptionJpaRepository>()
    private val chargeRepository = mockk<TossBillingChargeJpaRepository>(relaxed = true)
    private val attemptRepository = mockk<TossBillingChargeAttemptJpaRepository>(relaxed = true)
    private val methodRepository = mockk<TossBillingMethodJpaRepository>()
    private val tossPaymentsApiClient = mockk<TossPaymentsApiClient>()
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val notificationCommandService = mockk<NotificationCommandService>(relaxed = true)
    private val objectMapper = ObjectMapper()

    private lateinit var executor: BillingChargeExecutor

    @BeforeEach
    fun setUp() {
        executor =
            BillingChargeExecutor(
                subscriptionRepository = subscriptionRepository,
                chargeRepository = chargeRepository,
                attemptRepository = attemptRepository,
                methodRepository = methodRepository,
                tossPaymentsApiClient = tossPaymentsApiClient,
                userRepository = userRepository,
                notificationCommandService = notificationCommandService,
            )
    }

    @Test
    fun `같은 회차 DONE 이면 토스를 다시 호출하지 않는다`() {
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val subscription = subscription(id = 12L, nextBillingAt = due)
        val orderId = BillingChargeExecutor.periodOrderId(12L, due)
        val done =
            TossBillingCharge(
                id = 99L,
                userId = 1L,
                subscriptionId = 12L,
                billingMethod = subscription.billingMethod,
                orderId = orderId,
                orderName = "Glow Pass",
                amount = 4900,
                status = TossBillingChargeStatus.DONE,
            )
        every { subscriptionRepository.findWithBillingMethodById(12L) } returns subscription
        every { chargeRepository.findByOrderId(orderId) } returns done

        val result = executor.chargeSubscription(12L, due)

        assertEquals(TossBillingChargeStatus.DONE, result.status)
        verify(exactly = 0) {
            tossPaymentsApiClient.chargeBilling(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `회차 orderId 는 구독과 연월로 고정된다`() {
        val due = LocalDateTime.of(2026, 11, 15, 10, 0)
        assertEquals("sub-12-202611", BillingChargeExecutor.periodOrderId(12L, due))
        assertEquals("sub-12-202611-a2", BillingChargeExecutor.attemptOrderId("sub-12-202611", 2))
    }

    @Test
    fun `결제 실패 3회면 구독이 PAUSED 된다`() {
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val subscription = subscription(id = 12L, nextBillingAt = due)
        val orderId = BillingChargeExecutor.periodOrderId(12L, due)
        val pending = pendingCharge(7L, orderId, subscription.billingMethod)
        val attemptSlot = slot<TossBillingChargeAttempt>()
        stubFinalizeDeps(subscription, pending, attemptSlot)
        every {
            tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any())
        } returns
            TossApiResponse(
                400,
                objectMapper.readTree("""{"message":"한도초과"}"""),
                """{"message":"한도초과"}""",
            )

        subscription.failCount = 2
        runCatching { executor.finalizeWithToss(7L, due) }

        assertEquals(TossBillingSubscriptionStatus.PAUSED, subscription.status)
        assertTrue(pending.status == TossBillingChargeStatus.FAILED)
        assertEquals("sub-12-202611-a1", attemptSlot.captured.tossOrderId)
        assertEquals(TossBillingChargeStatus.FAILED, attemptSlot.captured.status)
    }

    @Test
    fun `실패 회차를 다른 카드로 재결제하면 시도 번호가 증가한다`() {
        val due = LocalDateTime.of(2026, 10, 1, 0, 0)
        val cardA = method(id = 3L, billingKey = "bk-a")
        val cardB = method(id = 4L, billingKey = "bk-b")
        val charge = failedCharge(cardA)
        val attemptSlot = slot<TossBillingChargeAttempt>()
        stubRetryDeps(charge, cardA, attemptSlot, due)

        executor.retryFailedCharge(9L, cardB, due)

        assertEquals(TossBillingChargeStatus.DONE, charge.status)
        assertEquals(4L, charge.billingMethod.id)
        assertEquals(2, attemptSlot.captured.attemptNo)
        assertEquals("sub-12-202610-a2", attemptSlot.captured.tossOrderId)
        assertEquals(TossBillingChargeStatus.DONE, attemptSlot.captured.status)
    }

    private fun stubFinalizeDeps(
        subscription: TossBillingSubscription,
        pending: TossBillingCharge,
        attemptSlot: io.mockk.CapturingSlot<TossBillingChargeAttempt>,
    ) {
        every { subscriptionRepository.findWithBillingMethodById(12L) } returns subscription
        every { chargeRepository.findByOrderId(pending.orderId) } returns pending andThen pending
        every { chargeRepository.findById(7L) } returns Optional.of(pending)
        every { attemptRepository.findTopByChargeIdOrderByAttemptNoDesc(7L) } returns null
        every { attemptRepository.save(capture(attemptSlot)) } answers { attemptSlot.captured }
        every { tossPaymentsApiClient.getPaymentByOrderId(any()) } returns TossApiResponse(404, null, null)
        every { userRepository.findById(1L) } returns testUser()
    }

    private fun stubRetryDeps(
        charge: TossBillingCharge,
        cardA: TossBillingMethod,
        attemptSlot: io.mockk.CapturingSlot<TossBillingChargeAttempt>,
        due: LocalDateTime,
    ) {
        val previous =
            TossBillingChargeAttempt(
                id = 1L,
                chargeId = 9L,
                billingMethod = cardA,
                attemptNo = 1,
                tossOrderId = "sub-12-202610-a1",
                status = TossBillingChargeStatus.FAILED,
            )
        every { chargeRepository.findById(9L) } returns Optional.of(charge)
        every { attemptRepository.findTopByChargeIdOrderByAttemptNoDesc(9L) } returns previous
        every { attemptRepository.save(capture(attemptSlot)) } answers { attemptSlot.captured }
        every { tossPaymentsApiClient.getPaymentByOrderId(any()) } returns TossApiResponse(404, null, null)
        every {
            tossPaymentsApiClient.chargeBilling(
                billingKey = "bk-b",
                customerKey = any(),
                amount = any(),
                orderId = "sub-12-202610-a2",
                orderName = any(),
                customerEmail = any(),
                customerName = any(),
                idempotencyKey = any(),
            )
        } returns
            TossApiResponse(
                200,
                objectMapper.readTree("""{"paymentKey":"pay-b","approvedAt":"2026-10-16T12:00:00+09:00"}"""),
                """{"paymentKey":"pay-b"}""",
            )
        every { userRepository.findById(1L) } returns testUser()
        every { subscriptionRepository.findWithBillingMethodById(12L) } returns
            subscription(id = 12L, nextBillingAt = due, method = cardA)
    }

    private fun testUser() =
        mockk<com.sleekydz86.idolglow.user.user.domain.User> {
            every { email } returns "a@b.com"
            every { nickname.value } returns "테스터"
        }

    private fun pendingCharge(
        id: Long,
        orderId: String,
        method: TossBillingMethod,
    ) = TossBillingCharge(
        id = id,
        userId = 1L,
        subscriptionId = 12L,
        billingMethod = method,
        orderId = orderId,
        orderName = "Glow Pass",
        amount = 4900,
        status = TossBillingChargeStatus.PENDING,
    )

    private fun failedCharge(method: TossBillingMethod) =
        TossBillingCharge(
            id = 9L,
            userId = 1L,
            subscriptionId = 12L,
            billingMethod = method,
            orderId = "sub-12-202610",
            orderName = "Glow Pass",
            amount = 4900,
            status = TossBillingChargeStatus.FAILED,
            failMessage = "한도초과",
        )

    private fun method(
        id: Long,
        billingKey: String,
    ): TossBillingMethod =
        TossBillingMethod(
            id = id,
            userId = 1L,
            customerKey = "idolglow-user-1",
            billingKey = billingKey,
            isDefault = id == 3L,
        )

    private fun subscription(
        id: Long,
        nextBillingAt: LocalDateTime,
        method: TossBillingMethod = method(id = 3L, billingKey = "bk"),
    ): TossBillingSubscription =
        TossBillingSubscription(
            id = id,
            userId = 1L,
            billingMethod = method,
            orderName = "Glow Pass",
            amount = 4900,
            nextBillingAt = nextBillingAt,
        )
}
