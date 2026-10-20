package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentRefund
import com.sleekydz86.idolglow.payment.domain.PaymentRefundStatus
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.domain.RefundRequestedBy
import com.sleekydz86.idolglow.payment.infrastructure.PaymentRefundJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal

@DisplayName("결제 환불")
class PaymentRefundServiceTest {
    private val paymentRepository = mockk<PaymentRepository>()
    private val paymentRefundJpaRepository = mockk<PaymentRefundJpaRepository>()
    private val tossPaymentsApiClient = mockk<TossPaymentsApiClient>()
    private val paymentLogCommandService = mockk<PaymentLogCommandService>(relaxed = true)
    private val paymentNotificationMailService = mockk<PaymentNotificationMailService>(relaxed = true)
    private val objectMapper = ObjectMapper()

    private lateinit var service: PaymentRefundService
    private val savedRefund = slot<PaymentRefund>()

    @BeforeEach
    fun setUp() {
        service =
            PaymentRefundService(
                paymentRepository = paymentRepository,
                paymentRefundJpaRepository = paymentRefundJpaRepository,
                tossPaymentsApiClient = tossPaymentsApiClient,
                paymentLogCommandService = paymentLogCommandService,
                tossPaymentProperties = TossPaymentProperties(enabled = true),
                objectMapper = objectMapper,
                paymentNotificationMailService = paymentNotificationMailService,
            )
        every { paymentRefundJpaRepository.save(capture(savedRefund)) } answers { savedRefund.captured }
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } returns
            TossApiResponse(
                500,
                objectMapper.readTree("""{"code":"PG_ERROR","message":"환불 실패"}"""),
                """{"message":"환불 실패"}""",
            )
    }

    @DisplayName("자동 환불이 토스에서 실패해도 예외 없이 실패 기록을 남긴다")
    @Test
    fun systemRefundKeepsFailedRecordWithoutThrowing() {
        // given
        val (payment, reservation) = succeededTossPayment(ReservationStatus.PENDING)

        // when
        service.refundAfterExpiredReservation(payment, reservation)

        // then
        assertEquals(PaymentRefundStatus.FAILED, savedRefund.captured.status)
        assertEquals(RefundRequestedBy.SYSTEM, savedRefund.captured.requestedBy)
        assertEquals(PaymentStatus.SUCCEEDED, payment.status)
    }

    @DisplayName("사용자 취소 환불이 토스에서 실패하면 지금처럼 예외로 취소를 막는다")
    @Test
    fun userRefundStillThrowsOnFailure() {
        // given
        val (payment, reservation) = succeededTossPayment(ReservationStatus.BOOKED)

        // when, then
        assertThrows<IllegalStateException> {
            service.refundBeforeReservationCancel(payment, reservation, "사용자 취소", RefundRequestedBy.USER)
        }
    }

    private fun succeededTossPayment(reservationStatus: ReservationStatus): Pair<Payment, Reservation> {
        val reservation = mockk<Reservation>(relaxed = true)
        every { reservation.totalPrice } returns BigDecimal("35000")
        every { reservation.status } returns reservationStatus
        val payment = Payment.createPendingForToss(reservation, "pay_toss_refund")
        payment.paymentKey = "pk-refund"
        payment.markSucceeded()
        return payment to reservation
    }
}
