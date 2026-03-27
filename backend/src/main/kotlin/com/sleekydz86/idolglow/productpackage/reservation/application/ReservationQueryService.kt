package com.sleekydz86.idolglow.productpackage.reservation.application

import com.sleekydz86.idolglow.productpackage.reservation.application.dto.ReservationSummaryResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import com.sleekydz86.idolglow.productpackage.reservation.infrastructure.ReservationQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class ReservationQueryService(
    private val reservationQueryRepository: ReservationQueryRepository
) {

    fun findReservationsByUser(userId: Long, today: LocalDate = LocalDate.now()): List<ReservationSummaryResponse> {
        val reservations = reservationQueryRepository.findByUserId(userId)
        return reservations.map { reservation ->
            val status = reservation.resolveStatus(today)
            ReservationSummaryResponse.from(reservation, status)
        }
    }

    fun findUpcomingReservationsByUser(userId: Long, today: LocalDate = LocalDate.now()): List<ReservationSummaryResponse> {
        val reservations = reservationQueryRepository.findByUserId(userId)
        return reservations
            .filter { it.visitDate.isEqual(today) || it.visitDate.isAfter(today) }
            .mapNotNull { reservation ->
                val status = reservation.resolveStatus(today)
                if (status == ReservationStatus.CANCELED || status == ReservationStatus.COMPLETED) {
                    null
                } else {
                    ReservationSummaryResponse.from(reservation, status)
                }
            }
    }

    fun findReservationDetail(reservationId: Long, userId: Long, today: LocalDate = LocalDate.now()): ReservationSummaryResponse {
        val reservation = reservationQueryRepository.findByIdAndUserId(reservationId, userId)
            ?: throw IllegalArgumentException("Reservation not found: $reservationId")
        return ReservationSummaryResponse.from(reservation, reservation.resolveStatus(today))
    }
}
