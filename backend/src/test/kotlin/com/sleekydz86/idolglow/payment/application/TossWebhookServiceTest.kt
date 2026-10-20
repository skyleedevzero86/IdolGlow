package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

@DisplayName("토스 웹훅")
class TossWebhookServiceTest {
    private val paymentRepository = mockk<PaymentRepository>()
    private val tossPaymentsApiClient = mockk<TossPaymentsApiClient>()
    private val paymentLogCommandService = mockk<PaymentLogCommandService>(relaxed = true)
    private val reservationPaymentService = mockk<ReservationPaymentService>()

    private lateinit var service: TossWebhookService

    @BeforeEach
    fun setUp() {
        service =
            TossWebhookService(
                objectMapper = ObjectMapper(),
                paymentRepository = paymentRepository,
                tossPaymentsApiClient = tossPaymentsApiClient,
                tossPaymentProperties = TossPaymentProperties(enabled = true),
                paymentLogCommandService = paymentLogCommandService,
                reservationPaymentService = reservationPaymentService,
            )
    }

    @DisplayName("웹훅도 결제를 직접 잠그지 않고 예약 → 결제 순서 잠금 경로를 쓴다")
    @Test
    fun webhookUsesReservationFirstLock() {
        // given
        val body = """{"eventType":"PAYMENT_STATUS_CHANGED","data":{"orderId":"ORD-1","paymentKey":"pk-1"}}"""
        every { reservationPaymentService.lockReservationThenPaymentByOrderId("ORD-1") } returns null

        // when
        service.handleSignedPayload(body)

        // then
        verify(exactly = 1) { reservationPaymentService.lockReservationThenPaymentByOrderId("ORD-1") }
        verify(exactly = 0) { paymentRepository.findByOrderIdForUpdate(any()) }
    }
}
