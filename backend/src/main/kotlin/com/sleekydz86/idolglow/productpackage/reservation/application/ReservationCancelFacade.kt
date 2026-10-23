package com.sleekydz86.idolglow.productpackage.reservation.application

import com.sleekydz86.idolglow.payment.application.PaymentLogCommandService
import com.sleekydz86.idolglow.payment.application.PaymentRefundService
import com.sleekydz86.idolglow.payment.application.RefundPrepareResult
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ReservationCancelFacade(
    private val reservationCommandService: ReservationCommandService,
    private val paymentRefundService: PaymentRefundService,
    private val paymentLogCommandService: PaymentLogCommandService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
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
        cancelToss(outcome)
        return outcome.reservationId
    }

    private fun cancelToss(outcome: ReservationCancelOutcome) {
        val refund = outcome.refund
        if (refund?.needsTossCancel != true) {
            return
        }
        runCatching { paymentRefundService.cancelPrepared(refund) }
            .onFailure { error -> recordRefundFailure(outcome.reservationId, refund, error) }
    }

    private fun recordRefundFailure(
        reservationId: Long,
        refund: RefundPrepareResult,
        error: Throwable,
    ) {
        log.error("환불 요청에 실패했습니다. reservationId={} paymentId={}", reservationId, refund.paymentId, error)
        paymentLogCommandService.append(
            payment = null,
            orderId = refund.orderId,
            paymentKey = refund.paymentKey,
            logType = PaymentLogType.CANCEL_ERROR,
            step = PaymentLogStep.SERVER,
            errorMessage = "환불 요청에 실패했습니다. reservationId=$reservationId paymentId=${refund.paymentId}",
            linkedPaymentId = refund.paymentId,
        )
    }
}
