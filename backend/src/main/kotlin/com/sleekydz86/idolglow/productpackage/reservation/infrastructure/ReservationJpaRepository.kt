package com.sleekydz86.idolglow.productpackage.reservation.infrastructure

import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface ReservationJpaRepository : JpaRepository<Reservation, Long> {

    @Query("select r from Reservation r join fetch r.reservationSlot rs join fetch rs.product where r.id = :id")
    fun findByIdWithSlotAndProduct(@Param("id") id: Long): Reservation?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r join fetch r.reservationSlot rs join fetch rs.product where r.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): Reservation?

    @Query("select r.id from Reservation r where r.status = :status and r.expiresAt is not null and r.expiresAt <= :now order by r.expiresAt asc")
    fun findExpiredPendingIds(
        @Param("status") status: ReservationStatus,
        @Param("now") now: LocalDateTime,
        pageable: Pageable
    ): List<Long>

    @Query("SELECT r.id FROM Reservation r WHERE r.status = 'PENDING' AND r.expiresAt > :now AND r.expiresAt <= :threshold")
    fun findExpiringSoonPendingIds(@Param("threshold") threshold: LocalDateTime, @Param("now") now: LocalDateTime): List<Long>

    @Query("select count(r) > 0 from Reservation r where r.reservationSlot.id = :reservationSlotId")
    fun existsByReservationSlotId(@Param("reservationSlotId") reservationSlotId: Long): Boolean

    @Query("select count(r) > 0 from Reservation r join r.reservationSlot rs where rs.product.id = :productId")
    fun existsByProductId(@Param("productId") productId: Long): Boolean

    @Query(
        "select count(r) > 0 from Reservation r where r.userId = :userId and r.reservationSlot.id = :slotId " +
            "and r.status in :statuses"
    )
    fun existsByUserIdAndReservationSlotIdAndStatusIn(
        @Param("userId") userId: Long,
        @Param("slotId") slotId: Long,
        @Param("statuses") statuses: Collection<ReservationStatus>,
    ): Boolean
}
