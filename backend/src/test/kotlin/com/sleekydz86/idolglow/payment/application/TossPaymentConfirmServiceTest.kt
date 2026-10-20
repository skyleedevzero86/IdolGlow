package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("토스 결제 승인")
class TossPaymentConfirmServiceTest {
    private val paymentRepository = mockk<PaymentRepository>()
    private val reservationPaymentService = mockk<ReservationPaymentService>()
    private val tossPaymentsApiClient = mockk<TossPaymentsApiClient>()
    private val paymentLogCommandService = mockk<PaymentLogCommandService>(relaxed = true)

    private lateinit var service: TossPaymentConfirmService

    @BeforeEach
    fun setUp() {
        service =
            TossPaymentConfirmService(
                paymentRepository = paymentRepository,
                reservationPaymentService = reservationPaymentService,
                tossPaymentsApiClient = tossPaymentsApiClient,
                paymentLogCommandService = paymentLogCommandService,
                tossPaymentProperties = TossPaymentProperties(enabled = true),
            )
    }

    @DisplayName("승인할 때 결제를 직접 잠그지 않고 예약 → 결제 순서 잠금 경로를 쓴다")
    @Test
    fun confirmUsesReservationFirstLock() {
        // given
        val payment = tossPayment()
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } returns
            TossApiResponse(400, null, """{"message":"승인 거절"}""")

        // when
        val result = service.confirm(10L, "pk-1", payment.orderId, BigDecimal("35000"), null, null)

        // then
        assertEquals(PaymentStatus.FAILED, result.status)
        verify(exactly = 1) { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) }
        verify(exactly = 0) { paymentRepository.findByOrderIdForUpdate(any()) }
    }

    @DisplayName("멱등 키 확인은 잠그지 않고 조회한다")
    @Test
    fun idempotencyCheckDoesNotLock() {
        // given
        val payment = tossPayment()
        every { paymentRepository.findByIdempotencyKey("idem-1") } returns null
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment
        every { tossPaymentsApiClient.confirm(any(), any(), any(), any()) } returns
            TossApiResponse(400, null, """{"message":"승인 거절"}""")

        // when
        service.confirm(10L, "pk-1", payment.orderId, BigDecimal("35000"), "idem-1", null)

        // then
        verify(exactly = 1) { paymentRepository.findByIdempotencyKey("idem-1") }
        verify(exactly = 0) { paymentRepository.findByIdempotencyKeyForUpdate(any()) }
    }

    @DisplayName("예약이 만료됐으면 토스 승인을 부르지 않고 결제를 만료 처리한다")
    @Test
    fun skipsTossConfirmWhenReservationExpired() {
        // given
        val payment = tossPayment(expired = true)
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment

        // when
        val result = service.confirm(10L, "pk-1", payment.orderId, BigDecimal("35000"), null, null)

        // then
        assertEquals(PaymentStatus.EXPIRED, result.status)
        verify(exactly = 0) { tossPaymentsApiClient.confirm(any(), any(), any(), any()) }
    }

    private fun tossPayment(expired: Boolean = false): Payment {
        val reservation = mockk<Reservation>(relaxed = true)
        every { reservation.userId } returns 10L
        every { reservation.totalPrice } returns BigDecimal("35000")
        every { reservation.status } returns ReservationStatus.PENDING
        every { reservation.isExpired(any()) } returns expired
        return Payment.createPendingForToss(reservation, "pay_toss_1")
    }
}
