package com.sleekydz86.idolglow.payment.application

import com.ninjasquad.springmockk.MockkBean
import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeAttemptJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
class BillingChargeTxIntegrationTest {
    @Autowired
    private lateinit var executor: BillingChargeExecutor

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var methodRepository: TossBillingMethodJpaRepository

    @Autowired
    private lateinit var subscriptionRepository: TossBillingSubscriptionJpaRepository

    @Autowired
    private lateinit var chargeRepository: TossBillingChargeJpaRepository

    @Autowired
    private lateinit var attemptRepository: TossBillingChargeAttemptJpaRepository

    @MockkBean
    private lateinit var tossPaymentsApiClient: TossPaymentsApiClient

    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun stubTossLookup() {
        clearMocks(tossPaymentsApiClient)
        every { tossPaymentsApiClient.getPaymentByOrderId(any()) } returns TossApiResponse(404, null, null)
    }

    @Test
    fun `자동결제가 실패하면 FAILED와 failCount가 DB에 남는다`() {
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val subscriptionId = persistActiveSubscription(due)
        stubTossChargeFailure()

        val thrown = runCatching { executor.chargeSubscription(subscriptionId, due) }

        assertTrue(thrown.isFailure)
        val sub = subscriptionRepository.findById(subscriptionId).orElseThrow()
        assertEquals(1, sub.failCount)
        assertNotNull(sub.retryAt)
        val charge =
            chargeRepository.findByOrderId(BillingChargeExecutor.periodOrderId(subscriptionId, due))!!
        assertEquals(TossBillingChargeStatus.FAILED, charge.status)
        val attempt = attemptRepository.findTopByChargeIdOrderByAttemptNoDesc(charge.id)!!
        assertEquals(TossBillingChargeStatus.FAILED, attempt.status)
    }

    @Test
    fun `같은 회차가 이미 DONE 이면 토스를 다시 호출하지 않는다`() {
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val subscriptionId = persistActiveSubscription(due)
        persistDoneCharge(subscriptionId, due)
        stubTossChargeSuccess()

        val result = executor.chargeSubscription(subscriptionId, due)

        assertEquals(TossBillingChargeStatus.DONE, result.status)
        verify(exactly = 0) {
            tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    private fun persistActiveSubscription(due: LocalDateTime): Long {
        val user =
            userJpaRepository.save(
                User(
                    email = "billing-${UUID.randomUUID()}@test.com",
                    nickname = Nickname.of("테스터"),
                ),
            )
        val method =
            methodRepository.save(
                TossBillingMethod(
                    userId = user.id,
                    customerKey = "idolglow-user-${user.id}",
                    billingKey = "bk-${user.id}",
                    isDefault = true,
                ),
            )
        val subscription =
            subscriptionRepository.save(
                TossBillingSubscription(
                    userId = user.id,
                    billingMethod = method,
                    orderName = BillingDefaults.DEFAULT_ORDER_NAME,
                    amount = BillingDefaults.MONTHLY_AMOUNT,
                    nextBillingAt = due,
                ),
            )
        return subscription.id
    }

    private fun persistDoneCharge(
        subscriptionId: Long,
        due: LocalDateTime,
    ) {
        val subscription = subscriptionRepository.findWithBillingMethodById(subscriptionId)!!
        chargeRepository.save(
            TossBillingCharge(
                userId = subscription.userId,
                subscriptionId = subscriptionId,
                billingMethod = subscription.billingMethod,
                orderId = BillingChargeExecutor.periodOrderId(subscriptionId, due),
                orderName = BillingDefaults.DEFAULT_ORDER_NAME,
                amount = BillingDefaults.MONTHLY_AMOUNT,
                status = TossBillingChargeStatus.DONE,
            ),
        )
    }

    private fun stubTossChargeFailure() {
        every {
            tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any())
        } returns
            TossApiResponse(
                400,
                objectMapper.readTree("""{"message":"한도초과"}"""),
                """{"message":"한도초과"}""",
            )
    }

    private fun stubTossChargeSuccess() {
        every {
            tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any())
        } returns
            TossApiResponse(
                200,
                objectMapper.readTree(
                    """{"paymentKey":"pay-ok","status":"DONE","approvedAt":"2026-11-01T00:00:00+09:00"}""",
                ),
                """{"paymentKey":"pay-ok","status":"DONE"}""",
            )
    }
}
