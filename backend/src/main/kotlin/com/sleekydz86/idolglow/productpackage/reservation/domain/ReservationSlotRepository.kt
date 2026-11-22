package com.sleekydz86.idolglow.productpackage.reservation.domain

interface ReservationSlotRepository {
    fun findById(slotId: Long): ReservationSlot?

    fun findByIdForUpdate(slotId: Long): ReservationSlot?

    fun findStateById(slotId: Long): ReservationSlotState?

    fun save(slot: ReservationSlot): ReservationSlot

    fun delete(slot: ReservationSlot)

    fun findAllByProductId(productId: Long): List<ReservationSlot>
}
