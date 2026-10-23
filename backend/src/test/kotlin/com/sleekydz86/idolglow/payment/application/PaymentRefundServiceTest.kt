package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.RefundRequestedBy
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
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal

@DisplayName("결제 환불")
class PaymentRefundServiceTest {
    private val paymentRefundTxService = mockk<PaymentRefundTxService>()
    private val tossPaymentsApiClient = mockk<TossPaymentsApiClient>()
    private val paymentLogCommandService = mockk<PaymentLogCommandService>(relaxed = true)
    private val objectMapper = ObjectMapper()

    private lateinit var service: PaymentRefundService

    @BeforeEach
    fun setUp() {
        service =
            PaymentRefundService(
                paymentRefundTxService = paymentRefundTxService,
                tossPaymentsApiClient = tossPaymentsApiClient,
                paymentLogCommandService = paymentLogCommandService,
                paymentRefundJpaRepository = mockk(),
            )
    }

    @DisplayName("만료 예약 자동 환불은 토스를 호출하지 않고 환불 준비만 한다")
    @Test
    fun systemRefundPreparesWithoutCallingToss() {
        // given
        val (payment, reservation) = succeededTossPayment(ReservationStatus.PENDING)
        val prepared = RefundPrepareResult(paymentId = payment.id, needsTossCancel = false)
        every {
            paymentRefundTxService.prepareRefund(payment.id, "결제 시간 초과로 자동 환불", RefundRequestedBy.SYSTEM)
        } returns prepared

        // when
        val result = service.refundAfterExpiredReservation(payment, reservation)

        // then
        assertEquals(prepared, result)
        verify(exactly = 0) { tossPaymentsApiClient.cancel(any(), any(), any(), any()) }
    }

    @DisplayName("사용자 환불 4xx는 예외 없이 완료 처리로 넘긴다")
    @Test
    fun userRefundFourXxDoesNotThrow() {
        // given
        val prepared = tossPrepared()
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } returns
            TossApiResponse(400, objectMapper.readTree("""{"code":"REJECT","message":"거절"}"""), """{"message":"거절"}""")
        every { paymentRefundTxService.completeRefund(prepared.refundId!!, any()) } returns Unit

        // when
        service.cancelPrepared(prepared)

        // then
        verify(exactly = 1) { paymentRefundTxService.completeRefund(prepared.refundId!!, any()) }
    }

    @DisplayName("토스 환불 5xx는 완료 처리를 호출하지 않는다")
    @Test
    fun inconclusiveRefundSkipsComplete() {
        // given
        val prepared = tossPrepared()
        every { tossPaymentsApiClient.cancel(any(), any(), any(), any()) } returns
            TossApiResponse(500, null, "internal")

        // when
        service.cancelPrepared(prepared)

        // then
        verify(exactly = 0) { paymentRefundTxService.completeRefund(any(), any()) }
    }

    private fun tossPrepared(): RefundPrepareResult =
        RefundPrepareResult(
            paymentId = 3L,
            orderId = "ORD-1",
            refundId = 9L,
            paymentKey = "pk-refund",
            idempotencyKey = "refund-9",
            cancelReason = "사용자 취소",
            needsTossCancel = true,
        )

    private fun succeededTossPayment(reservationStatus: ReservationStatus): Pair<Payment, Reservation> {
        val reservation = mockk<Reservation>(relaxed = true)
        every { reservation.id } returns 4L
        every { reservation.totalPrice } returns BigDecimal("35000")
        every { reservation.status } returns reservationStatus
        val payment = Payment.createPendingForToss(reservation, "pay_toss_refund")
        payment.paymentKey = "pk-refund"
        payment.markSucceeded()
        return payment to reservation
    }
}
