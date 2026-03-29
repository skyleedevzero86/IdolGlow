package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationSlotWaitlistService
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationCancelReason
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional
@Service
class ReservationPaymentService(
    private val paymentRepository: PaymentRepository,
    private val reservationRepository: ReservationRepository,
    private val reservationCommandService: ReservationCommandService,
    private val notificationCommandService: NotificationCommandService,
    private val reservationSlotWaitlistService: ReservationSlotWaitlistService,
) {

    fun handlePaymentSucceeded(paymentReference: String): Payment {
        val payment = findPaymentForUpdate(paymentReference)
        return finalizeAfterGatewaySuccess(payment)
    }

    fun finalizeAfterGatewaySuccess(payment: Payment): Payment {
        val reservation = findReservationForUpdate(payment.reservation.id)
        val now = LocalDateTime.now()

        if (payment.status == PaymentStatus.SUCCEEDED && reservation.status == ReservationStatus.BOOKED) {
            return payment
        }

        require(payment.status == PaymentStatus.PENDING || payment.status == PaymentStatus.SUCCEEDED) {
            "결제는 ${payment.status} 상태에서 확정할 수 없습니다."
        }

        if (reservation.status == ReservationStatus.CANCELED) {
            throw IllegalStateException("취소된 예약은 확정할 수 없습니다.")
        }

        if (reservation.isExpired(now)) {
            if (payment.status == PaymentStatus.PENDING) {
                payment.markExpired(now)
            }
            reservationCommandService.expirePendingReservation(reservation.id, now)
            return payment
        }

        if (payment.status == PaymentStatus.PENDING) {
            payment.markSucceeded(now)
        }

        if (reservation.status != ReservationStatus.BOOKED) {
            reservationCommandService.confirmReservation(reservation.id)
        }

        return payment
    }

    fun handlePaymentFailed(paymentReference: String, reason: String): Payment {
        val payment = findPaymentForUpdate(paymentReference)
        val reservation = findReservationForUpdate(payment.reservation.id)

        if (payment.status in setOf(PaymentStatus.FAILED, PaymentStatus.CANCELED, PaymentStatus.EXPIRED) &&
            reservation.status == ReservationStatus.CANCELED
        ) {
            return payment
        }

        require(payment.status == PaymentStatus.PENDING || payment.status == PaymentStatus.FAILED) {
            "결제 $paymentReference 는 ${payment.status} 상태에서 실패 처리할 수 없습니다."
        }

        if (payment.status == PaymentStatus.PENDING) {
            payment.markFailed(reason)
        }

        if (reservation.status != ReservationStatus.CANCELED) {
            reservation.cancel(ReservationCancelReason.PAYMENT_FAILED)
            notificationCommandService.create(
                userId = reservation.userId,
                type = NotificationType.PAYMENT_FAILED,
                title = "결제 실패",
                message = "예약 #${reservation.id} 결제에 실패했습니다.",
                link = "/reservations/${reservation.id}"
            )
            reservationSlotWaitlistService.notifyWaitersForReleasedSlot(reservation.reservationSlot.id)
        }

        return payment
    }

    fun handlePaymentCanceled(paymentReference: String, reason: String): Payment {
        val payment = findPaymentForUpdate(paymentReference)
        val reservation = findReservationForUpdate(payment.reservation.id)

        if (payment.status in setOf(PaymentStatus.FAILED, PaymentStatus.CANCELED, PaymentStatus.EXPIRED) &&
            reservation.status == ReservationStatus.CANCELED
        ) {
            return payment
        }

        require(payment.status == PaymentStatus.PENDING || payment.status == PaymentStatus.CANCELED) {
            "결제 $paymentReference 는 ${payment.status} 상태에서 취소 처리할 수 없습니다."
        }

        if (payment.status == PaymentStatus.PENDING) {
            payment.markCanceled(reason)
        }

        if (reservation.status != ReservationStatus.CANCELED) {
            reservation.cancel(ReservationCancelReason.PAYMENT_FAILED)
            notificationCommandService.create(
                userId = reservation.userId,
                type = NotificationType.PAYMENT_FAILED,
                title = "결제 취소",
                message = "예약 #${reservation.id} 결제가 취소되었습니다.",
                link = "/reservations/${reservation.id}"
            )
            reservationSlotWaitlistService.notifyWaitersForReleasedSlot(reservation.reservationSlot.id)
        }

        return payment
    }

    private fun findPaymentForUpdate(paymentReference: String): Payment =
        paymentRepository.findByPaymentReferenceForUpdate(paymentReference)
            ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentReference")

    private fun findReservationForUpdate(reservationId: Long) =
        reservationRepository.findByIdForUpdate(reservationId)
            ?: throw IllegalArgumentException("예약을 찾을 수 없습니다: $reservationId")
}
