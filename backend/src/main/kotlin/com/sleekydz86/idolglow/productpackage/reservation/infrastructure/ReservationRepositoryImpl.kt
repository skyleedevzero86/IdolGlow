package com.sleekydz86.idolglow.productpackage.reservation.infrastructure

import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ReservationRepositoryImpl(
    private val reservationJpaRepository: ReservationJpaRepository
) : ReservationRepository {

    override fun save(reservation: Reservation): Reservation =
        reservationJpaRepository.save(reservation)

    override fun findById(id: Long): Reservation? =
        reservationJpaRepository.findByIdOrNull(id)

    override fun findByIdWithSlotAndProduct(id: Long): Reservation? =
        reservationJpaRepository.findByIdWithSlotAndProduct(id)

    override fun findByIdForUpdate(id: Long): Reservation? =
        reservationJpaRepository.findByIdForUpdate(id)

    override fun findExpiredPendingIds(limit: Int, now: LocalDateTime): List<Long> =
        reservationJpaRepository.findExpiredPendingIds(
            status = ReservationStatus.PENDING,
            now = now,
            pageable = PageRequest.of(0, limit)
        )

    override fun findExpiringSoonPendingIds(threshold: LocalDateTime, now: LocalDateTime): List<Long> =
        reservationJpaRepository.findExpiringSoonPendingIds(threshold = threshold, now = now)

    override fun existsByReservationSlotId(reservationSlotId: Long): Boolean =
        reservationJpaRepository.existsByReservationSlotId(reservationSlotId)

    override fun existsByProductId(productId: Long): Boolean =
        reservationJpaRepository.existsByProductId(productId)
}
