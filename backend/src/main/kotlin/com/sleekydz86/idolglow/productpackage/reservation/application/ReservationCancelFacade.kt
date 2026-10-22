package com.sleekydz86.idolglow.productpackage.reservation.application

import com.sleekydz86.idolglow.payment.application.PaymentRefundService
import com.sleekydz86.idolglow.payment.application.RefundPrepareResult
import org.springframework.stereotype.Service

@Service
class ReservationCancelFacade(
    private val reservationCommandService: ReservationCommandService,
    private val paymentRefundService: PaymentRefundService,
) {
    fun cancelByUser(
        reservationId: Long,
        userId: Long,
        cancelReasonText: String? = null,
    ): Long = settle(reservationCommandService.cancelReservationByUser(reservationId, userId, cancelReasonText))

    fun cancelMyPagePayment(
        userId: Long,
        paymentId: Long,
        cancelReasonText: String? = null,
    ): Long = settle(reservationCommandService.cancelMyPagePayment(userId, paymentId, cancelReasonText))

    fun cancelByAdmin(
        reservationId: Long,
        cancelReasonText: String? = null,
    ): Long = settle(reservationCommandService.cancelReservationByAdmin(reservationId, cancelReasonText))

    fun cancelAdminPayment(
        paymentId: Long,
        cancelReasonText: String? = null,
    ): Long = settle(reservationCommandService.cancelAdminPayment(paymentId, cancelReasonText))

    private fun settle(outcome: ReservationCancelOutcome): Long {
        cancelToss(outcome.refund)
        return outcome.reservationId
    }

    private fun cancelToss(refund: RefundPrepareResult?) {
        if (refund?.needsTossCancel == true) {
            paymentRefundService.cancelPrepared(refund)
        }
    }
}
