package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PaymentViewPolicyService {

    fun canAdminCancel(payment: Payment): Boolean =
        payment.reservation.status != ReservationStatus.CANCELED &&
            payment.status in setOf(PaymentStatus.PENDING, PaymentStatus.SUCCEEDED)

    fun userCancelDeadline(payment: Payment): LocalDateTime? {
        if (payment.status != PaymentStatus.SUCCEEDED) {
            return null
        }
        val base = payment.approvedAt ?: payment.createdAt ?: return null
        return base.plusDays(USER_CANCEL_WINDOW_DAYS)
    }

    fun canUserCancel(payment: Payment, now: LocalDateTime = LocalDateTime.now()): Boolean {
        val deadline = userCancelDeadline(payment) ?: return false
        return payment.reservation.status != ReservationStatus.CANCELED && !deadline.isBefore(now)
    }

    fun receiptAvailable(payment: Payment): Boolean =
        payment.status in setOf(
            PaymentStatus.SUCCEEDED,
            PaymentStatus.CANCELED,
            PaymentStatus.REFUNDED,
            PaymentStatus.PARTIAL_CANCELED,
        )

    companion object {
        const val USER_CANCEL_WINDOW_DAYS = 15L
    }
}
