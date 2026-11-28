package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.application.dto.MyGlowPassStatsResponse
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import com.sleekydz86.idolglow.payment.infrastructure.PaymentJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationCancelReason
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode

@Transactional(readOnly = true)
@Service
class MyGlowPassStatsService(
    private val chargeRepository: TossBillingChargeJpaRepository,
    private val paymentRepository: PaymentJpaRepository,
    private val benefitPolicy: GlowPassBenefitPolicy,
) {
    fun stats(userId: Long): MyGlowPassStatsResponse {
        val doneCharges = chargeRepository.findAllByUserIdOrderByIdDesc(userId).filter { it.status == DONE }
        val payments = paymentRepository.findWithReservationByUserIdAndStatusIn(userId, PAYMENT_STATUSES)
        val booked = payments.map { it.reservation }.filter { it.isBooked() }
        val refunded = payments.filter { it.status == PaymentStatus.REFUNDED }.map { it.reservation }
        return MyGlowPassStatsResponse(
            membershipSince = doneCharges.mapNotNull { it.approvedAt }.minOrNull()?.toLocalDate(),
            totalPaid = doneCharges.sumOf { it.amount },
            paidCount = doneCharges.size,
            savedByDiscount = booked.sumOf { it.membershipDiscount }.setScale(0, RoundingMode.DOWN).toLong(),
            earlyBookings = booked.count { benefitPolicy.isEarlyBooking(it) },
            freeCancels = refunded.count { it.isUserCanceled() && benefitPolicy.isFreeOnlyForMember(it) },
        )
    }

    private fun Reservation.isBooked(): Boolean = status in BOOKED_STATUSES

    private fun Reservation.isUserCanceled(): Boolean =
        status == ReservationStatus.CANCELED && cancelReason == ReservationCancelReason.USER_REQUESTED

    private companion object {
        val DONE = TossBillingChargeStatus.DONE
        val PAYMENT_STATUSES = listOf(PaymentStatus.SUCCEEDED, PaymentStatus.REFUNDED)
        val BOOKED_STATUSES = setOf(ReservationStatus.BOOKED, ReservationStatus.COMPLETED)
    }
}
