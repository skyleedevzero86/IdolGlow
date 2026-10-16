package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import io.mockk.every
import io.mockk.mockk
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
        verify(exactly = 0) { tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `회차 orderId 는 구독과 연월로 고정된다`() {
        val due = LocalDateTime.of(2026, 11, 15, 10, 0)
        assertEquals("sub-12-202611", BillingChargeExecutor.periodOrderId(12L, due))
    }

    @Test
    fun `결제 실패 3회면 구독이 PAUSED 된다`() {
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val subscription = subscription(id = 12L, nextBillingAt = due)
        val orderId = BillingChargeExecutor.periodOrderId(12L, due)
        val pending =
            TossBillingCharge(
                id = 7L,
                userId = 1L,
                subscriptionId = 12L,
                billingMethod = subscription.billingMethod,
                orderId = orderId,
                orderName = "Glow Pass",
                amount = 4900,
                status = TossBillingChargeStatus.PENDING,
            )
        every { subscriptionRepository.findWithBillingMethodById(12L) } returns subscription
        every { chargeRepository.findByOrderId(orderId) } returns pending andThen pending
        every { chargeRepository.findById(7L) } returns Optional.of(pending)
        every { tossPaymentsApiClient.getPaymentByOrderId(orderId) } returns TossApiResponse(404, null, null)
        every {
            tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any())
        } returns TossApiResponse(400, objectMapper.readTree("""{"message":"한도초과"}"""), """{"message":"한도초과"}""")
        every { userRepository.findById(1L) } returns
            mockk {
                every { email } returns "a@b.com"
                every { nickname.value } returns "테스터"
            }

        subscription.failCount = 2
        runCatching { executor.finalizeWithToss(7L, due) }

        assertEquals(TossBillingSubscriptionStatus.PAUSED, subscription.status)
        assertTrue(pending.status == TossBillingChargeStatus.FAILED)
    }

    private fun subscription(
        id: Long,
        nextBillingAt: LocalDateTime,
    ): TossBillingSubscription {
        val method =
            TossBillingMethod(
                id = 3L,
                userId = 1L,
                customerKey = "idolglow-user-1",
                billingKey = "bk",
            )
        return TossBillingSubscription(
            id = id,
            userId = 1L,
            billingMethod = method,
            orderName = "Glow Pass",
            amount = 4900,
            nextBillingAt = nextBillingAt,
        )
    }
}
