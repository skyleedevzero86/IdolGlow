package com.sleekydz86.idolglow.payment.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.payment.adapter.web.TossBillingController
import com.sleekydz86.idolglow.payment.adapter.web.TossBillingOneOffChargeController
import com.sleekydz86.idolglow.payment.application.dto.StartTossBillingSubscriptionRequest
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeAttemptJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingCustomerJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingDueClaimRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.assertj.AssertableApplicationContext
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@DisplayName("정기결제 구독 금액")
class TossBillingSubscriptionAmountTest {
    private val methodRepository = mockk<TossBillingMethodJpaRepository>()
    private val subscriptionRepository = mockk<TossBillingSubscriptionJpaRepository>()
    private lateinit var service: TossBillingService

    @BeforeEach
    fun setUp() {
        val method =
            TossBillingMethod(
                id = 3L,
                userId = USER_ID,
                customerKey = "idolglow-user-1",
                billingKey = "billing-key",
                isDefault = true,
            )
        every { methodRepository.findByUserIdAndActiveTrueAndIsDefaultTrue(USER_ID) } returns method
        service =
            TossBillingService(
                tossPaymentProperties =
                    TossPaymentProperties(
                        enabled = true,
                        secretKey = "test-secret",
                        clientKey = "test-client",
                    ),
                tossPaymentsApiClient = mockk<TossPaymentsApiClient>(relaxed = true),
                methodRepository = methodRepository,
                customerRepository = mockk<TossBillingCustomerJpaRepository>(relaxed = true),
                subscriptionRepository = subscriptionRepository,
                chargeRepository = mockk<TossBillingChargeJpaRepository>(relaxed = true),
                attemptRepository = mockk<TossBillingChargeAttemptJpaRepository>(relaxed = true),
                dueClaimRepository = mockk<TossBillingDueClaimRepository>(relaxed = true),
                billingChargeExecutor = mockk<BillingChargeExecutor>(relaxed = true),
                billingKeyDeletionService = mockk(relaxed = true),
                self = mockk<TossBillingService>(relaxed = true),
                frontendBaseUrl = "http://localhost:3000",
                defaultAmount = BillingDefaults.MONTHLY_AMOUNT,
                defaultOrderName = BillingDefaults.DEFAULT_ORDER_NAME,
            )
    }

    @DisplayName("새 구독은 클라이언트가 보낸 금액을 무시하고 서버 금액을 저장한다")
    @Test
    fun subscriptionAmountIgnoresClientValue() {
        // given
        every { subscriptionRepository.findByUserIdAndPlanCode(USER_ID, "MONTHLY") } returns null
        val saved = slot<TossBillingSubscription>()
        every { subscriptionRepository.saveAndFlush(capture(saved)) } answers { saved.captured }

        // when
        service.saveSubscription(USER_ID, requestWithClientAmount(100L))

        // then
        assertThat(saved.captured.amount).isEqualTo(BillingDefaults.MONTHLY_AMOUNT)
    }

    @DisplayName("이미 있는 구독도 클라이언트가 보낸 금액으로 바꾸지 않는다")
    @Test
    fun existingSubscriptionRenewalIgnoresClientValue() {
        // given
        val existing =
            TossBillingSubscription(
                id = 9L,
                userId = USER_ID,
                billingMethod =
                    TossBillingMethod(
                        id = 3L,
                        userId = USER_ID,
                        customerKey = "idolglow-user-1",
                        billingKey = "billing-key",
                        isDefault = true,
                    ),
                orderName = BillingDefaults.DEFAULT_ORDER_NAME,
                amount = 100L,
            )
        every { subscriptionRepository.findByUserIdAndPlanCode(USER_ID, "MONTHLY") } returns existing
        every { subscriptionRepository.saveAndFlush(existing) } returns existing

        // when
        service.saveSubscription(USER_ID, requestWithClientAmount(100L))

        // then
        assertThat(existing.amount).isEqualTo(BillingDefaults.MONTHLY_AMOUNT)
    }

    @DisplayName("단건 청구는 운영 프로필에 없고 테스트 프로필에만 있다")
    @Test
    fun oneOffChargeDisabledInProdProfile() {
        // given

        // when
        profileRunner("prod").run { context: AssertableApplicationContext ->
            // then
            assertThat(context).hasNotFailed()
            assertThat(context).hasSingleBean(TossBillingController::class.java)
            assertThat(context).doesNotHaveBean(TossBillingOneOffChargeController::class.java)
        }
        profileRunner("test").run { context ->
            // then
            assertThat(context).hasNotFailed()
            assertThat(context).hasSingleBean(TossBillingController::class.java)
            assertThat(context).hasSingleBean(TossBillingOneOffChargeController::class.java)
        }
    }

    private fun requestWithClientAmount(amount: Long): StartTossBillingSubscriptionRequest {
        val mapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        return mapper.readValue(
            """{"amount":$amount,"orderName":"${BillingDefaults.DEFAULT_ORDER_NAME}","chargeImmediately":false}""",
            StartTossBillingSubscriptionRequest::class.java,
        )
    }

    private fun profileRunner(profile: String): ApplicationContextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(BillingControllerProfileSlice::class.java)
            .withPropertyValues("spring.profiles.active=$profile")

    @Configuration
    @Import(TossBillingController::class, TossBillingOneOffChargeController::class)
    class BillingControllerProfileSlice {
        @Bean
        fun tossBillingService(): TossBillingService = mockk(relaxed = true)
    }

    companion object {
        private const val USER_ID: Long = 1L
    }
}
