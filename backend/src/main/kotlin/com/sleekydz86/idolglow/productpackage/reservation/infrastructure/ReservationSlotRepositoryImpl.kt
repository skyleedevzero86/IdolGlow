package com.sleekydz86.idolglow.productpackage.reservation.infrastructure

import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlotRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class ReservationSlotRepositoryImpl(
    private val reservationSlotJpaRepository: ReservationSlotJpaRepository
) : ReservationSlotRepository {

    override fun findById(slotId: Long): ReservationSlot? =
        reservationSlotJpaRepository.findByIdOrNull(slotId)

    override fun findByIdForUpdate(slotId: Long): ReservationSlot? =
        reservationSlotJpaRepository.findByIdForUpdate(slotId)

    override fun save(slot: ReservationSlot): ReservationSlot =
        reservationSlotJpaRepository.save(slot)

    override fun delete(slot: ReservationSlot) =
        reservationSlotJpaRepository.delete(slot)

    override fun findAllByProductId(productId: Long): List<ReservationSlot> =
        reservationSlotJpaRepository.findAllByProductIdOrderByReservationDateAscStartTimeAsc(productId)
}
