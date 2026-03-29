package com.sleekydz86.idolglow.productpackage.reservation.infrastructure

import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReservationSlotJpaRepository : JpaRepository<ReservationSlot, Long> {

    @Query("select s from ReservationSlot s join fetch s.product where s.id = :id")
    fun findWithProductById(@Param("id") id: Long): ReservationSlot?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rs from ReservationSlot rs join fetch rs.product where rs.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): ReservationSlot?

    fun findAllByProductIdOrderByReservationDateAscStartTimeAsc(productId: Long): List<ReservationSlot>
}
