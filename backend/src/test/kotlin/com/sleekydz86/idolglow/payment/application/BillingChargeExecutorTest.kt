package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.Optional

@DisplayName("빌링 회차 결제 실행기")
class BillingChargeExecutorTest {
    private val subscriptionRepository = mockk<TossBillingSubscriptionJpaRepository>()
    private val chargeRepository = mockk<TossBillingChargeJpaRepository>(relaxed = true)
    private val attemptRepository = mockk<TossBillingChargeAttemptJpaRepository>(relaxed = true)
    private val methodRepository = mockk<TossBillingMethodJpaRepository>()
    private val tossPaymentsApiClient = mockk<TossPaymentsApiClient>()
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val notificationCommandService = mockk<NotificationCommandService>(relaxed = true)
    private val objectMapper = ObjectMapper()

    private lateinit var txService: BillingChargeTxService
    private lateinit var executor: BillingChargeExecutor

    @BeforeEach
    fun setUp() {
        txService =
            BillingChargeTxService(
                subscriptionRepository = subscriptionRepository,
                chargeRepository = chargeRepository,
                attemptRepository = attemptRepository,
                methodRepository = methodRepository,
                tossPaymentsApiClient = tossPaymentsApiClient,
                userRepository = userRepository,
            )
        executor = BillingChargeExecutor(txService, notificationCommandService)
    }

    @DisplayName("같은 회차 DONE 이면 토스를 다시 호출하지 않는다")
    @Test
    fun skipsTossWhenPeriodAlreadyDone() {
        // given
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
                orderName = BillingDefaults.DEFAULT_ORDER_NAME,
                amount = BillingDefaults.MONTHLY_AMOUNT,
                status = TossBillingChargeStatus.DONE,
            )
        every { subscriptionRepository.findWithBillingMethodById(12L) } returns subscription
        every { chargeRepository.findByOrderId(orderId) } returns done

        // when
        val result = executor.chargeSubscription(12L, due)

        // then
        assertEquals(TossBillingChargeStatus.DONE, result.status)
        verify(exactly = 0) {
            tossPaymentsApiClient.chargeBilling(
                billingKey = any(),
                customerKey = any(),
                amount = any(),
                orderId = any(),
                orderName = any(),
                customerEmail = any(),
                customerName = any(),
                idempotencyKey = any(),
            )
        }
        verify(exactly = 0) { notificationCommandService.create(any(), any(), any(), any(), any()) }
    }

    @DisplayName("예전 형식 키가 저장된 카드 청구는 그 카드의 키를 그대로 쓴다")
    @Test
    fun existingCardChargeUsesStoredMethodKey() {
        // given
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val storedKey = "idolglow-user-1"
        val card = method(id = 3L, billingKey = "bk", customerKey = storedKey)
        val subscription = subscription(id = 12L, nextBillingAt = due, method = card)
        val orderId = BillingChargeExecutor.periodOrderId(12L, due)
        val pending = pendingCharge(7L, orderId, card)
        val attemptSlot = slot<TossBillingChargeAttempt>()
        stubFinalizeDeps(subscription, pending, attemptSlot)
        every {
            tossPaymentsApiClient.chargeBilling(
                billingKey = "bk",
                customerKey = storedKey,
                amount = BillingDefaults.MONTHLY_AMOUNT,
                orderId = "sub-12-202611-a1",
                orderName = any(),
                customerEmail = any(),
                customerName = any(),
                idempotencyKey = any(),
            )
        } returns
            TossApiResponse(
                200,
                objectMapper.readTree("""{"paymentKey":"pay-old","approvedAt":"2026-11-01T12:00:00+09:00"}"""),
                """{"paymentKey":"pay-old"}""",
            )

        // when
        executor.chargeSubscription(12L, due)

        // then
        assertEquals(storedKey, card.customerKey)
        verify(exactly = 1) {
            tossPaymentsApiClient.chargeBilling(
                billingKey = "bk",
                customerKey = storedKey,
                amount = BillingDefaults.MONTHLY_AMOUNT,
                orderId = "sub-12-202611-a1",
                orderName = any(),
                customerEmail = any(),
                customerName = any(),
                idempotencyKey = any(),
            )
        }
    }

    @DisplayName("회차 orderId 는 구독과 연월로 고정된다")
    @Test
    fun periodOrderIdUsesSubscriptionAndYearMonth() {
        // given
        // when
        val due = LocalDateTime.of(2026, 11, 15, 10, 0)
        // then
        assertEquals("sub-12-202611", BillingChargeExecutor.periodOrderId(12L, due))
        assertEquals("sub-12-202611-a2", BillingChargeExecutor.attemptOrderId("sub-12-202611", 2))
    }

    @DisplayName("결제 실패 3회면 구독이 PAUSED 된다")
    @Test
    fun pausesSubscriptionAfterThreeFailures() {
        // given
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val subscription = subscription(id = 12L, nextBillingAt = due)
        val orderId = BillingChargeExecutor.periodOrderId(12L, due)
        val pending = pendingCharge(7L, orderId, subscription.billingMethod)
        val attemptSlot = slot<TossBillingChargeAttempt>()
        stubFinalizeDeps(subscription, pending, attemptSlot)
        every {
            tossPaymentsApiClient.chargeBilling(
                billingKey = "bk",
                customerKey = "idolglow-user-1",
                amount = BillingDefaults.MONTHLY_AMOUNT,
                orderId = "sub-12-202611-a1",
                orderName = any(),
                customerEmail = any(),
                customerName = any(),
                idempotencyKey = any(),
            )
        } returns
            TossApiResponse(
                400,
                objectMapper.readTree("""{"message":"한도초과"}"""),
                """{"message":"한도초과"}""",
            )

        subscription.failCount = 2

        // when
        val thrown = runCatching { executor.chargeSubscription(12L, due) }

        // then
        assertEquals("한도초과", thrown.exceptionOrNull()?.message)
        assertEquals(TossBillingSubscriptionStatus.PAUSED, subscription.status)
        assertTrue(pending.status == TossBillingChargeStatus.FAILED)
        assertEquals("sub-12-202611-a1", attemptSlot.captured.tossOrderId)
        assertEquals(TossBillingChargeStatus.FAILED, attemptSlot.captured.status)
        verify(exactly = 1) {
            notificationCommandService.create(any(), NotificationType.PAYMENT_FAILED, any(), any(), any())
        }
    }

    @DisplayName("실패 회차를 다른 카드로 재결제하면 시도 번호가 증가한다")
    @Test
    fun incrementsAttemptNoOnRetryWithOtherCard() {
        // given
        val due = LocalDateTime.of(2026, 10, 1, 0, 0)
        val cardA = method(id = 3L, billingKey = "bk-a")
        val cardB = method(id = 4L, billingKey = "bk-b")
        val charge = failedCharge(cardA)
        val attemptSlot = slot<TossBillingChargeAttempt>()
        stubRetryDeps(charge, cardA, attemptSlot, due)

        // when
        executor.retryFailedCharge(9L, cardB, due)

        // then
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
                customerKey = "idolglow-user-1",
                amount = BillingDefaults.MONTHLY_AMOUNT,
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
        orderName = BillingDefaults.DEFAULT_ORDER_NAME,
        amount = BillingDefaults.MONTHLY_AMOUNT,
        status = TossBillingChargeStatus.PENDING,
    )

    private fun failedCharge(method: TossBillingMethod) =
        TossBillingCharge(
            id = 9L,
            userId = 1L,
            subscriptionId = 12L,
            billingMethod = method,
            orderId = "sub-12-202610",
            orderName = BillingDefaults.DEFAULT_ORDER_NAME,
            amount = BillingDefaults.MONTHLY_AMOUNT,
            status = TossBillingChargeStatus.FAILED,
            failMessage = "한도초과",
        )

    private fun method(
        id: Long,
        billingKey: String,
        customerKey: String = "idolglow-user-1",
    ): TossBillingMethod =
        TossBillingMethod(
            id = id,
            userId = 1L,
            customerKey = customerKey,
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
            orderName = BillingDefaults.DEFAULT_ORDER_NAME,
            amount = BillingDefaults.MONTHLY_AMOUNT,
            nextBillingAt = nextBillingAt,
        )
}
