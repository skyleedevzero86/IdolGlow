package com.sleekydz86.idolglow.payment.application

import com.ninjasquad.springmockk.MockkBean
import com.sleekydz86.idolglow.notification.domain.NotificationRepository
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.UUID

@DisplayName("빌링 실패 알림 PostgreSQL")
class BillingNotificationRollbackPostgresTest : PostgresIntegrationTest() {
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

    @MockkBean
    private lateinit var tossPaymentsApiClient: TossPaymentsApiClient

    @MockkBean
    private lateinit var notificationRepository: NotificationRepository

    private val objectMapper = ObjectMapper()

    @DisplayName("실제 알림 서비스가 저장 실패해도 FAILED와 failCount는 DB에 남는다")
    @Test
    fun keepsFailedStatusWhenRealNotificationSaveFails() {
        val due = LocalDateTime.of(2026, 11, 1, 0, 0)
        val subscriptionId = persistActiveSubscription(due)
        every { tossPaymentsApiClient.getPaymentByOrderId(any()) } returns TossApiResponse(404, null, null)
        every {
            tossPaymentsApiClient.chargeBilling(any(), any(), any(), any(), any(), any(), any(), any())
        } returns
            TossApiResponse(
                400,
                objectMapper.readTree("""{"message":"한도초과"}"""),
                """{"message":"한도초과"}""",
            )
        every { notificationRepository.save(any()) } throws IllegalStateException("알림 저장 실패")

        val thrown = runCatching { executor.chargeSubscription(subscriptionId, due) }

        assertEquals("한도초과", thrown.exceptionOrNull()?.message)
        val sub = subscriptionRepository.findById(subscriptionId).orElseThrow()
        assertEquals(1, sub.failCount)
        val charge = chargeRepository.findByOrderId(BillingChargeExecutor.periodOrderId(subscriptionId, due))!!
        assertEquals(TossBillingChargeStatus.FAILED, charge.status)
        assertTrue(thrown.isFailure)
    }

    private fun persistActiveSubscription(due: LocalDateTime): Long {
        val token = UUID.randomUUID().toString().replace("-", "").take(8)
        val user =
            userJpaRepository.save(
                User(
                    email = "bn$token@test.com",
                    nickname = Nickname.of("b$token"),
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
}
