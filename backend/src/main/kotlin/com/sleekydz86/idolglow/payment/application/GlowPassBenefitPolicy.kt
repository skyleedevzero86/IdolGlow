package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class GlowPassBenefitPolicy(
    private val membershipQuery: MembershipQuery,
    @param:Value("\${payment.toss.billing.glow-pass.public-open-days-before:7}")
    private val publicOpenDaysBefore: Long,
    @param:Value("\${payment.toss.billing.glow-pass.early-booking-hours:24}")
    private val earlyBookingHours: Long,
    @param:Value("\${payment.toss.billing.glow-pass.free-cancel-days-member:1}")
    private val freeCancelDaysMember: Long,
    @param:Value("\${payment.toss.billing.glow-pass.free-cancel-days-normal:3}")
    private val freeCancelDaysNormal: Long,
) {
    fun assertEarlyBookingAllowed(
        userId: Long,
        slot: ReservationSlot,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        val publicOpenAt = publicOpensAt(slot)
        if (!now.isBefore(publicOpenAt)) {
            return
        }
        val memberOpenAt = memberOpensAt(slot)
        val member = membershipQuery.isActive(userId, now)
        if (member && !now.isBefore(memberOpenAt)) {
            return
        }
        if (now.isBefore(memberOpenAt)) {
            throw IllegalArgumentException(
                "아직 예약 오픈 전입니다. memberOpenAt=$memberOpenAt publicOpenAt=$publicOpenAt now=$now",
            )
        }
        throw IllegalArgumentException(
            "일반 예약 오픈 전입니다. Glow Pass 회원은 ${earlyBookingHours}시간 전부터 예약할 수 있습니다. publicOpenAt=$publicOpenAt",
        )
    }

    fun assertFreeCancelAllowed(
        userId: Long,
        reservation: Reservation,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        val visitAt = LocalDateTime.of(reservation.visitDate, reservation.visitStartTime)
        val member = membershipQuery.isActive(userId, now)
        val minDays = if (member) freeCancelDaysMember else freeCancelDaysNormal
        val deadline = visitAt.minusDays(minDays)
        require(!now.isAfter(deadline)) {
            "무료 취소는 방문 ${minDays}일 전까지만 가능합니다. deadline=$deadline now=$now member=$member"
        }
    }

    fun publicOpensAt(slot: ReservationSlot): LocalDateTime =
        LocalDateTime.of(slot.reservationDate, slot.startTime).minusDays(publicOpenDaysBefore)

    fun memberOpensAt(slot: ReservationSlot): LocalDateTime = publicOpensAt(slot).minusHours(earlyBookingHours)
}
