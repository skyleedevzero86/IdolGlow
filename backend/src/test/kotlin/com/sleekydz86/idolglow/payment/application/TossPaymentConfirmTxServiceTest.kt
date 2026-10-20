package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal

@DisplayName("토스 결제 승인 트랜잭션")
class TossPaymentConfirmTxServiceTest {
    private val paymentRepository = mockk<PaymentRepository>()
    private val reservationPaymentService = mockk<ReservationPaymentService>()
    private lateinit var service: TossPaymentConfirmTxService

    @BeforeEach
    fun setUp() {
        service =
            TossPaymentConfirmTxService(
                paymentRepository = paymentRepository,
                reservationPaymentService = reservationPaymentService,
                tossPaymentProperties = TossPaymentProperties(enabled = true),
            )
    }

    @DisplayName("승인 준비는 결제를 직접 잠그지 않고 예약 → 결제 순서 잠금 경로를 쓴다")
    @Test
    fun prepareUsesReservationFirstLock() {
        val payment = tossPayment()
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment

        val result = service.prepareConfirm(10L, payment.orderId, BigDecimal("35000"), null)

        assertEquals(PaymentStatus.CONFIRMING, result.payment.status)
        verify(exactly = 1) { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) }
        verify(exactly = 0) { paymentRepository.findByOrderIdForUpdate(any()) }
    }

    @DisplayName("멱등 키 확인은 잠그지 않고 조회한다")
    @Test
    fun idempotencyCheckDoesNotLock() {
        val payment = tossPayment()
        every { paymentRepository.findByIdempotencyKey("idem-1") } returns null
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment

        service.prepareConfirm(10L, payment.orderId, BigDecimal("35000"), "idem-1")

        verify(exactly = 1) { paymentRepository.findByIdempotencyKey("idem-1") }
        verify(exactly = 0) { paymentRepository.findByIdempotencyKeyForUpdate(any()) }
    }

    @DisplayName("토스 승인 실패면 실패 상태로 남긴다")
    @Test
    fun completeMarksFailedWhenTossRejects() {
        val payment = tossPayment()
        payment.markConfirming()
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment

        val result =
            service.completeConfirm(
                10L,
                payment.orderId,
                BigDecimal("35000"),
                TossApiResponse(400, null, """{"message":"승인 거절"}"""),
            )

        assertEquals(PaymentStatus.FAILED, result.status)
    }

    @DisplayName("토스 승인이 성공하면 예약 만료와 관계없이 승인 확정으로 보낸다")
    @Test
    fun completeAppliesSuccessWithoutExpireCheckWhenTossApproves() {
        val payment = tossPayment(expired = true)
        payment.markConfirming()
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment
        every { reservationPaymentService.finalizeAfterGatewaySuccess(payment) } returns payment

        val result =
            service.completeConfirm(
                10L,
                payment.orderId,
                BigDecimal("35000"),
                tossDoneResponse(),
            )

        assertEquals(PaymentStatus.SUCCEEDED, result.status)
        verify(exactly = 1) { reservationPaymentService.finalizeAfterGatewaySuccess(payment) }
    }

    @DisplayName("토스가 거절했고 예약이 닫혀 있으면 결제를 만료한다")
    @Test
    fun completeMarksExpiredWhenTossRejectsClosedReservation() {
        val payment = tossPayment(expired = true)
        payment.markConfirming()
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment

        val result =
            service.completeConfirm(
                10L,
                payment.orderId,
                BigDecimal("35000"),
                TossApiResponse(400, null, """{"message":"승인 거절"}"""),
            )

        assertEquals(PaymentStatus.EXPIRED, result.status)
    }

    @DisplayName("조회로 결론이 안 난 처리 중 결제는 실패로 넘긴다")
    @Test
    fun failUnresolvedConfirmingDelegatesToPaymentFailed() {
        val payment = tossPayment()
        payment.markConfirming()
        every { reservationPaymentService.lockReservationThenPaymentByOrderId(payment.orderId) } returns payment
        every {
            reservationPaymentService.handlePaymentFailed(payment.paymentReference, "토스에 주문이 없습니다.")
        } returns payment

        service.failUnresolvedConfirming(10L, payment.orderId, BigDecimal("35000"), "토스에 주문이 없습니다.")

        verify(exactly = 1) {
            reservationPaymentService.handlePaymentFailed(payment.paymentReference, "토스에 주문이 없습니다.")
        }
    }

    private fun tossDoneResponse(): TossApiResponse {
        val json =
            ObjectMapper().readTree(
                """{"paymentKey":"pk-ok","status":"DONE","approvedAt":"2026-10-20T21:30:00+09:00"}""",
            )
        return TossApiResponse(200, json, """{"status":"DONE"}""")
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
