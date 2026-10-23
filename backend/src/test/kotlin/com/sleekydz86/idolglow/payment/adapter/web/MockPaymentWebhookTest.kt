package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.global.infrastructure.exception.GlobalExceptionAdvice
import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.payment.adapter.graphql.PaymentGraphQlController
import com.sleekydz86.idolglow.payment.adapter.web.request.MockPaymentWebhookRequest
import com.sleekydz86.idolglow.payment.adapter.web.request.MockPaymentWebhookStatus
import com.sleekydz86.idolglow.payment.application.PaymentNotificationMailService
import com.sleekydz86.idolglow.payment.application.PaymentRefundService
import com.sleekydz86.idolglow.payment.application.ReservationPaymentService
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationSlotWaitlistService
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.assertj.AssertableApplicationContext
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("모의 결제 웹훅")
class MockPaymentWebhookTest {
    @DisplayName("시크릿이 비어 있으면 모의 웹훅을 거절하고 서버는 빈을 만든다")
    @Test
    fun mockWebhookRejectsWhenSecretBlank() {
        // given
        val payment = tossPayment()
        val (service, reservationRepository) = paymentService(payment)
        val mockMvc =
            MockMvcBuilders
                .standaloneSetup(PaymentController(service, ""))
                .setControllerAdvice(GlobalExceptionAdvice())
                .build()

        // when
        mockMvc
            .perform(
                post("/payments/mock/webhook")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"paymentReference":"pay_mock","status":"SUCCEEDED"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("webhook-secret")))

        // then
        verify(exactly = 0) { reservationRepository.findByIdForUpdate(any()) }
    }

    @DisplayName("토스 결제 참조는 모의 웹훅에서 거절한다")
    @Test
    fun mockWebhookRejectsTossPayment() {
        // given
        val payment = tossPayment()
        val (service, reservationRepository) = paymentService(payment)
        val mockMvc =
            MockMvcBuilders
                .standaloneSetup(PaymentController(service, WEBHOOK_SECRET))
                .setControllerAdvice(GlobalExceptionAdvice())
                .build()

        // when
        mockMvc
            .perform(
                post("/payments/mock/webhook")
                    .header("X-Mock-Payment-Secret", WEBHOOK_SECRET)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"paymentReference":"pay_toss","status":"SUCCEEDED"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("MOCK")))

        // then
        verify(exactly = 0) { reservationRepository.findByIdForUpdate(any()) }
    }

    @DisplayName("모의 결제 웹훅은 운영 프로필에 없고 테스트 프로필에만 있다")
    @Test
    fun mockWebhookDisabledInProdProfile() {
        // given

        // when
        profileRunner("prod").run { context: AssertableApplicationContext ->
            // then
            assertThat(context).hasNotFailed()
            assertThat(context).doesNotHaveBean(PaymentController::class.java)
            assertThat(context).doesNotHaveBean(PaymentGraphQlController::class.java)
        }

        profileRunner("test", "payment.mock.webhook-secret=$WEBHOOK_SECRET").run { context ->
            // then
            assertThat(context).hasNotFailed()
            assertThat(context).hasSingleBean(PaymentController::class.java)
            assertThat(context).hasSingleBean(PaymentGraphQlController::class.java)
        }
    }

    @DisplayName("그래프큐엘 모의 웹훅도 토스 결제 참조는 거절한다")
    @Test
    fun graphQlMockWebhookRejectsTossPayment() {
        // given
        val payment = tossPayment()
        val (service, reservationRepository) = paymentService(payment)
        val controller = PaymentGraphQlController(service, WEBHOOK_SECRET)

        // when
        val exception =
            assertThrows<IllegalArgumentException> {
                controller.mockPaymentWebhook(
                    secret = WEBHOOK_SECRET,
                    input =
                        MockPaymentWebhookRequest(
                            paymentReference = "pay_toss",
                            status = MockPaymentWebhookStatus.SUCCEEDED,
                        ),
                )
            }

        // then
        assertThat(exception.message).contains("MOCK")
        verify(exactly = 0) { reservationRepository.findByIdForUpdate(any()) }
    }

    private fun profileRunner(
        profile: String,
        vararg extraProperties: String,
    ): ApplicationContextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(MockWebhookProfileSlice::class.java)
            .withPropertyValues("spring.profiles.active=$profile", *extraProperties)

    private fun tossPayment(): Payment {
        val visitDate = LocalDate.now().plusDays(3)
        val product =
            Product.createWithTimeSlots(
                name = "테스트 상품",
                description = "설명",
                options = emptyList(),
                tagNames = emptyList(),
                slotStartDate = visitDate,
                slotEndDate = visitDate,
            )
        val slot = product.reservationSlots.first()
        val expiresAt = LocalDateTime.now().plusMinutes(15)
        slot.hold(reservationId = 1L, expiresAt = expiresAt)
        val reservation =
            Reservation(
                id = 1L,
                reservationSlot = slot,
                userId = 10L,
                visitDate = slot.reservationDate,
                visitStartTime = slot.startTime,
                visitEndTime = slot.endTime,
                totalPrice = BigDecimal("35000"),
            ).request(expiresAt)
        return Payment.createPendingForToss(reservation, "pay_toss")
    }

    private fun paymentService(payment: Payment): Pair<ReservationPaymentService, ReservationRepository> {
        val paymentRepository = mockk<PaymentRepository>()
        every { paymentRepository.findByPaymentReference(payment.paymentReference) } returns payment
        val reservationRepository = mockk<ReservationRepository>()
        val service =
            ReservationPaymentService(
                paymentRepository = paymentRepository,
                reservationRepository = reservationRepository,
                reservationCommandService = mockk<ReservationCommandService>(relaxed = true),
                notificationCommandService = mockk<NotificationCommandService>(relaxed = true),
                reservationSlotWaitlistService = mockk<ReservationSlotWaitlistService>(relaxed = true),
                paymentNotificationMailService = mockk<PaymentNotificationMailService>(relaxed = true),
                paymentRefundService = mockk<PaymentRefundService>(relaxed = true),
                reservationPaymentLock = mockk(relaxed = true),
            )
        return service to reservationRepository
    }

    @Configuration
    @Import(PaymentController::class, PaymentGraphQlController::class)
    class MockWebhookProfileSlice {
        @Bean
        fun reservationPaymentService(): ReservationPaymentService = mockk(relaxed = true)
    }

    companion object {
        private const val WEBHOOK_SECRET: String = "test-mock-webhook-secret"
    }
}
