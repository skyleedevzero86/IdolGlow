package com.sleekydz86.idolglow.survey.application.autopick

import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationBookingChannel
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import com.sleekydz86.idolglow.survey.infrastructure.autopick.SurveyAutoPickCandidateJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class SurveyAutoPickMonthlyCounter(
    private val candidateRepository: SurveyAutoPickCandidateJpaRepository,
) {
    fun bookedInMonth(
        userId: Long,
        now: LocalDateTime,
    ): Long {
        val monthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay()
        return candidateRepository.countConfirmedReservations(
            userId = userId,
            channel = ReservationBookingChannel.AUTO_PICK,
            statuses = BOOKED_STATUSES,
            from = monthStart,
            to = monthStart.plusMonths(1),
        )
    }

    companion object {
        const val MONTHLY_LIMIT = 2
        private val BOOKED_STATUSES = listOf(ReservationStatus.BOOKED, ReservationStatus.COMPLETED)
    }
}
