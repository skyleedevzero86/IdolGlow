package com.sleekydz86.idolglow.payment.application

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
) {

    fun handlePaymentSucceeded(paymentReference: String): Payment {
        val payment = findPaymentForUpdate(paymentReference)
        val reservation = findReservationForUpdate(payment.reservation.id)
        val now = LocalDateTime.now()

        if (payment.status == PaymentStatus.SUCCEEDED && reservation.status == ReservationStatus.BOOKED) {
            return payment
        }

        require(payment.status == PaymentStatus.PENDING || payment.status == PaymentStatus.SUCCEEDED) {
            "Payment $paymentReference cannot be marked as succeeded from ${payment.status}."
        }

        if (reservation.status == ReservationStatus.CANCELED) {
            throw IllegalStateException("Canceled reservation cannot be confirmed.")
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
            "Payment $paymentReference cannot fail from ${payment.status}."
        }

        if (payment.status == PaymentStatus.PENDING) {
            payment.markFailed(reason)
        }

        if (reservation.status != ReservationStatus.CANCELED) {
            reservation.cancel(ReservationCancelReason.PAYMENT_FAILED)
            notificationCommandService.create(
                userId = reservation.userId,
                type = NotificationType.PAYMENT_FAILED,
                title = "Payment failed",
                message = "Payment for reservation #${reservation.id} failed.",
                link = "/reservations/${reservation.id}"
            )
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
            "Payment $paymentReference cannot be canceled from ${payment.status}."
        }

        if (payment.status == PaymentStatus.PENDING) {
            payment.markCanceled(reason)
        }

        if (reservation.status != ReservationStatus.CANCELED) {
            reservation.cancel(ReservationCancelReason.PAYMENT_FAILED)
            notificationCommandService.create(
                userId = reservation.userId,
                type = NotificationType.PAYMENT_FAILED,
                title = "Payment canceled",
                message = "Payment for reservation #${reservation.id} was canceled.",
                link = "/reservations/${reservation.id}"
            )
        }

        return payment
    }

    private fun findPaymentForUpdate(paymentReference: String): Payment =
        paymentRepository.findByPaymentReferenceForUpdate(paymentReference)
            ?: throw IllegalArgumentException("Payment not found: $paymentReference")

    private fun findReservationForUpdate(reservationId: Long) =
        reservationRepository.findByIdForUpdate(reservationId)
            ?: throw IllegalArgumentException("Reservation not found: $reservationId")
}
