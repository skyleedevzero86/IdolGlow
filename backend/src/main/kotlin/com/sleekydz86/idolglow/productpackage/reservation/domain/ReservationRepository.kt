package com.sleekydz86.idolglow.productpackage.reservation.domain

interface ReservationRepository {
    fun save(reservation: Reservation): Reservation
    fun findById(id: Long): Reservation?
    fun findByIdWithSlotAndProduct(id: Long): Reservation?
    fun findByIdForUpdate(id: Long): Reservation?
    fun findExpiredPendingIds(limit: Int, now: java.time.LocalDateTime): List<Long>
    fun existsByReservationSlotId(reservationSlotId: Long): Boolean
    fun existsByProductId(productId: Long): Boolean
}
