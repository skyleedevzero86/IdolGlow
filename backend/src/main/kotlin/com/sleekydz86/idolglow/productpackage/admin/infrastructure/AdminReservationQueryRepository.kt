package com.sleekydz86.idolglow.productpackage.admin.infrastructure

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminReservationPaymentProjection
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class AdminReservationQueryRepository(
    private val entityManager: EntityManager,
) {

    fun findReservations(
        status: ReservationStatus?,
        visitDate: LocalDate?,
        productId: Long?,
        size: Int,
    ): List<Reservation> {
        val conditions = mutableListOf<String>()
        if (status != null) {
            conditions += "r.status = :status"
        }
        if (visitDate != null) {
            conditions += "r.visitDate = :visitDate"
        }
        if (productId != null) {
            conditions += "p.id = :productId"
        }

        val whereClause = if (conditions.isEmpty()) "" else "where ${conditions.joinToString(" and ")}"

        val query = entityManager.createQuery(
            """
            select r from Reservation r
            join fetch r.reservationSlot rs
            join fetch rs.product p
            $whereClause
            order by r.createdAt desc
            """.trimIndent(),
            Reservation::class.java
        )
            .setMaxResults(size)

        if (status != null) {
            query.setParameter("status", status)
        }
        if (visitDate != null) {
            query.setParameter("visitDate", visitDate)
        }
        if (productId != null) {
            query.setParameter("productId", productId)
        }

        return query.resultList
    }

    fun findRecentReservations(fromDate: LocalDate?, toDate: LocalDate?, size: Int): List<Reservation> {
        val conditions = mutableListOf<String>()
        if (fromDate != null) {
            conditions += "r.visitDate >= :fromDate"
        }
        if (toDate != null) {
            conditions += "r.visitDate <= :toDate"
        }

        val whereClause = if (conditions.isEmpty()) "" else "where ${conditions.joinToString(" and ")}"

        val query = entityManager.createQuery(
            """
            select r from Reservation r
            join fetch r.reservationSlot rs
            join fetch rs.product p
            $whereClause
            order by r.createdAt desc
            """.trimIndent(),
            Reservation::class.java
        )
            .setMaxResults(size)

        if (fromDate != null) {
            query.setParameter("fromDate", fromDate)
        }
        if (toDate != null) {
            query.setParameter("toDate", toDate)
        }

        return query.resultList
    }

    fun countReservationsByStatus(fromDate: LocalDate?, toDate: LocalDate?): Map<ReservationStatus, Long> {
        @Suppress("UNCHECKED_CAST")
        val rows = createDateRangeQuery(
            """
            select r.status, count(r.id)
            from Reservation r
            %s
            group by r.status
            """.trimIndent(),
            fromDate,
            toDate
        ).resultList as List<Array<Any>>

        return rows.associate { row ->
            row[0] as ReservationStatus to (row[1] as Number).toLong()
        }
    }

    fun countReservationDashboard(today: LocalDate, fromDate: LocalDate?, toDate: LocalDate?): ReservationDashboardCounts {
        val whereClause = buildNativeVisitDateClause(fromDate, toDate)
        val query = entityManager.createNativeQuery(
            """
            select
                coalesce(sum(case when r.status = 'PENDING' then 1 else 0 end), 0),
                coalesce(sum(case when r.status = 'BOOKED' and r.visit_date >= :today then 1 else 0 end), 0),
                coalesce(sum(case when r.status = 'BOOKED' and r.visit_date < :today then 1 else 0 end), 0),
                coalesce(sum(case when r.status = 'CANCELED' then 1 else 0 end), 0)
            from reservations r
            $whereClause
            """.trimIndent()
        )
            .setParameter("today", today)

        if (fromDate != null) {
            query.setParameter("fromDate", fromDate)
        }
        if (toDate != null) {
            query.setParameter("toDate", toDate)
        }

        val row = query.singleResult as Array<Any>
        return ReservationDashboardCounts(
            pendingCount = (row[0] as Number).toLong(),
            bookedCount = (row[1] as Number).toLong(),
            completedCount = (row[2] as Number).toLong(),
            canceledCount = (row[3] as Number).toLong()
        )
    }

    fun countPaymentsByStatus(fromDate: LocalDate?, toDate: LocalDate?): Map<PaymentStatus, Long> {
        @Suppress("UNCHECKED_CAST")
        val rows = createDateRangeQuery(
            """
            select p.status, count(p.id)
            from Payment p
            join p.reservation r
            %s
            group by p.status
            """.trimIndent(),
            fromDate,
            toDate
        ).resultList as List<Array<Any>>

        return rows.associate { row ->
            row[0] as PaymentStatus to (row[1] as Number).toLong()
        }
    }

    fun findPaymentsByVisitDateRange(fromDate: LocalDate, toDate: LocalDate, max: Int): List<Payment> {
        return entityManager.createQuery(
            """
            select p from Payment p
            join fetch p.reservation r
            join fetch r.reservationSlot rs
            join fetch rs.product prod
            where r.visitDate >= :fromDate and r.visitDate <= :toDate
            order by p.id desc
            """.trimIndent(),
            Payment::class.java
        )
            .setParameter("fromDate", fromDate)
            .setParameter("toDate", toDate)
            .setMaxResults(max)
            .resultList
    }

    fun findReservationsByVisitDateRange(fromDate: LocalDate, toDate: LocalDate, max: Int): List<Reservation> {
        return entityManager.createQuery(
            """
            select r from Reservation r
            join fetch r.reservationSlot rs
            join fetch rs.product p
            where r.visitDate >= :fromDate and r.visitDate <= :toDate
            order by r.id desc
            """.trimIndent(),
            Reservation::class.java
        )
            .setParameter("fromDate", fromDate)
            .setParameter("toDate", toDate)
            .setMaxResults(max)
            .resultList
    }

    fun findPaymentsByReservationIds(reservationIds: List<Long>): Map<Long, AdminReservationPaymentProjection> {
        if (reservationIds.isEmpty()) {
            return emptyMap()
        }

        @Suppress("UNCHECKED_CAST")
        val rows = entityManager.createQuery(
            """
            select p.reservation.id, p.paymentReference, p.status, p.failureReason
            from Payment p
            where p.reservation.id in :reservationIds
            """.trimIndent()
        )
            .setParameter("reservationIds", reservationIds)
            .resultList as List<Array<Any>>

        return rows.associate { row ->
            val reservationId = (row[0] as Number).toLong()
            reservationId to AdminReservationPaymentProjection(
                reservationId = reservationId,
                paymentReference = row[1] as String,
                status = row[2] as PaymentStatus,
                failureReason = row[3] as String?
            )
        }
    }

    private fun createDateRangeQuery(baseQuery: String, fromDate: LocalDate?, toDate: LocalDate?) =
        entityManager.createQuery(
            baseQuery.format(buildDateRangeClause(fromDate, toDate))
        ).also { query ->
            if (fromDate != null) {
                query.setParameter("fromDate", fromDate)
            }
            if (toDate != null) {
                query.setParameter("toDate", toDate)
            }
        }

    private fun buildDateRangeClause(fromDate: LocalDate?, toDate: LocalDate?): String {
        val conditions = mutableListOf<String>()
        if (fromDate != null) {
            conditions += "r.visitDate >= :fromDate"
        }
        if (toDate != null) {
            conditions += "r.visitDate <= :toDate"
        }
        return if (conditions.isEmpty()) "" else "where ${conditions.joinToString(" and ")}"
    }

    private fun buildNativeVisitDateClause(fromDate: LocalDate?, toDate: LocalDate?): String {
        val conditions = mutableListOf<String>()
        if (fromDate != null) {
            conditions += "r.visit_date >= :fromDate"
        }
        if (toDate != null) {
            conditions += "r.visit_date <= :toDate"
        }
        return if (conditions.isEmpty()) "" else "where ${conditions.joinToString(" and ")}"
    }

    data class ReservationDashboardCounts(
        val pendingCount: Long,
        val bookedCount: Long,
        val completedCount: Long,
        val canceledCount: Long,
    )
}
