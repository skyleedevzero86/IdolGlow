package com.sleekydz86.idolglow.survey.infrastructure.autopick

import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationBookingChannel
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickCandidate
import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.time.LocalDateTime

interface SurveyAutoPickCandidateJpaRepository : JpaRepository<SurveyAutoPickCandidate, Long> {
    fun existsByUserIdAndReservationSlotId(
        userId: Long,
        reservationSlotId: Long,
    ): Boolean

    fun findTop20ByUserIdOrderByScheduledChargeAtDesc(userId: Long): List<SurveyAutoPickCandidate>

    @Query(
        "select c.id from SurveyAutoPickCandidate c where c.status = :status and c.scheduledChargeAt <= :now " +
            "order by c.scheduledChargeAt, c.id",
    )
    fun findDueIds(
        @Param("status") status: SurveyAutoPickStatus,
        @Param("now") now: LocalDateTime,
    ): List<Long>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from SurveyAutoPickCandidate c where c.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): SurveyAutoPickCandidate?

    @Query(
        "select s from ReservationSlot s join fetch s.product p " +
            "where p.id in :productIds and s.reservationDate between :fromDate and :toDate",
    )
    fun findSlotsOfProducts(
        @Param("productIds") productIds: Collection<Long>,
        @Param("fromDate") fromDate: LocalDate,
        @Param("toDate") toDate: LocalDate,
    ): List<ReservationSlot>

    @Query(
        "select count(r) from Reservation r where r.userId = :userId and r.bookingChannel = :channel " +
            "and r.status in :statuses and r.confirmedAt >= :from and r.confirmedAt < :to",
    )
    fun countConfirmedReservations(
        @Param("userId") userId: Long,
        @Param("channel") channel: ReservationBookingChannel,
        @Param("statuses") statuses: Collection<ReservationStatus>,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): Long
}
