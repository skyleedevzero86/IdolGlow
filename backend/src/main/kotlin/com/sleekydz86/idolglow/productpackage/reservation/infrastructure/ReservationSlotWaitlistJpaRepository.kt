package com.sleekydz86.idolglow.productpackage.reservation.infrastructure

import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlotWaitlistEntry
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface ReservationSlotWaitlistJpaRepository : JpaRepository<ReservationSlotWaitlistEntry, Long> {

    @EntityGraph(attributePaths = ["reservationSlot.product"])
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<ReservationSlotWaitlistEntry>

    fun findAllByReservationSlotIdOrderByCreatedAtAsc(reservationSlotId: Long): List<ReservationSlotWaitlistEntry>

    fun deleteByReservationSlotId(reservationSlotId: Long): Long

    fun deleteByUserIdAndReservationSlotId(userId: Long, reservationSlotId: Long): Long

    fun existsByUserIdAndReservationSlotId(userId: Long, reservationSlotId: Long): Boolean
}
