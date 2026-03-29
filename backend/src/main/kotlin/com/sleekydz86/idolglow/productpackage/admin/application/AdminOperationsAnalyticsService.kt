package com.sleekydz86.idolglow.productpackage.admin.application

import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.productpackage.admin.application.dto.CancelReasonStatRow
import com.sleekydz86.idolglow.productpackage.admin.application.dto.CancellationComparisonResponse
import com.sleekydz86.idolglow.productpackage.admin.application.dto.OperationsAnalyticsSummaryResponse
import com.sleekydz86.idolglow.productpackage.admin.application.dto.PaymentFailureHourRow
import com.sleekydz86.idolglow.productpackage.admin.application.dto.PeriodCancellationMetrics
import com.sleekydz86.idolglow.productpackage.admin.application.dto.ProductConversionRow
import com.sleekydz86.idolglow.productpackage.admin.application.dto.SlotHourOccupancyRow
import com.sleekydz86.idolglow.productpackage.admin.application.dto.computeRate
import com.sleekydz86.idolglow.productpackage.admin.infrastructure.AdminReservationQueryRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Transactional(readOnly = true)
@Service
class AdminOperationsAnalyticsService(
    private val entityManager: EntityManager,
    private val adminReservationQueryRepository: AdminReservationQueryRepository,
) {

    fun summary(visitDateFrom: LocalDate, visitDateTo: LocalDate): OperationsAnalyticsSummaryResponse {
        val canceled = countReservationsByStatusAndVisitRange(ReservationStatus.CANCELED, visitDateFrom, visitDateTo)
        val booked = countReservationsByStatusAndVisitRange(ReservationStatus.BOOKED, visitDateFrom, visitDateTo)
        val payMap = adminReservationQueryRepository.countPaymentsByStatus(visitDateFrom, visitDateTo)
        val payStrings = payMap.mapKeys { it.key.name }
        return OperationsAnalyticsSummaryResponse(
            visitDateFrom = visitDateFrom,
            visitDateTo = visitDateTo,
            reservationCanceled = canceled,
            reservationBooked = booked,
            cancelRate = computeRate(canceled, canceled + booked),
            paymentStatusCounts = payStrings,
        )
    }

    fun cancellationComparison(visitDateFrom: LocalDate, visitDateTo: LocalDate): CancellationComparisonResponse {
        val days = ChronoUnit.DAYS.between(visitDateFrom, visitDateTo) + 1L
        val prevTo = visitDateFrom.minusDays(1)
        val prevFrom = visitDateFrom.minusDays(days)
        val current = periodCancellationMetrics(visitDateFrom, visitDateTo)
        val previous = periodCancellationMetrics(prevFrom, prevTo)
        val delta = when {
            current.cancelRate != null && previous.cancelRate != null ->
                current.cancelRate!!.subtract(previous.cancelRate!!)
            else -> null
        }
        return CancellationComparisonResponse(current = current, previous = previous, cancelRateDelta = delta)
    }

    fun cancelReasonStats(canceledAtFrom: LocalDateTime, canceledAtTo: LocalDateTime): List<CancelReasonStatRow> {
        @Suppress("UNCHECKED_CAST")
        val rows = entityManager.createQuery(
            """
            select r.cancelReason, count(r.id)
            from Reservation r
            where r.status = :st
              and r.canceledAt is not null
              and r.canceledAt >= :cf
              and r.canceledAt <= :ct
            group by r.cancelReason
            """.trimIndent()
        )
            .setParameter("st", ReservationStatus.CANCELED)
            .setParameter("cf", canceledAtFrom)
            .setParameter("ct", canceledAtTo)
            .resultList as List<Array<Any>>

        return rows.map { row ->
            val reason = row[0] as? Enum<*>
            CancelReasonStatRow(reason = reason?.name, count = (row[1] as Number).toLong())
        }.sortedByDescending { it.count }
    }

    fun productConversion(visitDateFrom: LocalDate, visitDateTo: LocalDate): List<ProductConversionRow> {
        @Suppress("UNCHECKED_CAST")
        val rows = entityManager.createQuery(
            """
            select p.id, p.name,
                sum(case when r.status = :booked then 1 else 0 end),
                sum(case when r.status = :canceled then 1 else 0 end)
            from Reservation r
            join r.reservationSlot rs
            join rs.product p
            where r.visitDate >= :vf and r.visitDate <= :vt
            group by p.id, p.name
            """.trimIndent()
        )
            .setParameter("booked", ReservationStatus.BOOKED)
            .setParameter("canceled", ReservationStatus.CANCELED)
            .setParameter("vf", visitDateFrom)
            .setParameter("vt", visitDateTo)
            .resultList as List<Array<Any>>

        return rows.map { row ->
            val booked = (row[2] as Number).toLong()
            val canceled = (row[3] as Number).toLong()
            ProductConversionRow(
                productId = (row[0] as Number).toLong(),
                productName = row[1] as String,
                booked = booked,
                canceled = canceled,
                conversionRate = computeRate(booked, booked + canceled),
            )
        }.sortedByDescending { it.booked + it.canceled }
    }

    fun slotOccupancyByHour(visitDateFrom: LocalDate, visitDateTo: LocalDate): List<SlotHourOccupancyRow> {
        val slots = entityManager.createQuery(
            """
            select rs from ReservationSlot rs
            where rs.reservationDate >= :vf and rs.reservationDate <= :vt
            """.trimIndent(),
            ReservationSlot::class.java
        )
            .setParameter("vf", visitDateFrom)
            .setParameter("vt", visitDateTo)
            .resultList as List<ReservationSlot>

        val byHour = slots.groupBy { it.startTime.hour }
        return byHour.keys.sorted().map { hour ->
            val list = byHour[hour].orEmpty()
            val total = list.size.toLong()
            val booked = list.count { it.isStatusBooked }.toLong()
            SlotHourOccupancyRow(
                hourOfDay = hour,
                totalSlots = total,
                bookedSlots = booked,
                occupancyRate = computeRate(booked, total),
            )
        }
    }

    fun paymentFailuresByHour(failedAtFrom: LocalDateTime, failedAtTo: LocalDateTime): List<PaymentFailureHourRow> {
        @Suppress("UNCHECKED_CAST")
        val times = entityManager.createQuery(
            """
            select p.failedAt from Payment p
            where p.status = :st
              and p.failedAt is not null
              and p.failedAt >= :ff
              and p.failedAt <= :ft
            """.trimIndent()
        )
            .setParameter("st", PaymentStatus.FAILED)
            .setParameter("ff", failedAtFrom)
            .setParameter("ft", failedAtTo)
            .resultList as List<LocalDateTime>

        val byHour = times.groupBy { it.hour }
        return byHour.keys.sorted().map { hour ->
            PaymentFailureHourRow(hourOfDay = hour, failureCount = byHour[hour]!!.size.toLong())
        }
    }

    private fun periodCancellationMetrics(from: LocalDate, to: LocalDate): PeriodCancellationMetrics {
        val canceled = countReservationsByStatusAndVisitRange(ReservationStatus.CANCELED, from, to)
        val booked = countReservationsByStatusAndVisitRange(ReservationStatus.BOOKED, from, to)
        return PeriodCancellationMetrics(
            fromDate = from,
            toDate = to,
            canceled = canceled,
            booked = booked,
            cancelRate = computeRate(canceled, canceled + booked),
        )
    }

    private fun countReservationsByStatusAndVisitRange(
        status: ReservationStatus,
        from: LocalDate,
        to: LocalDate,
    ): Long {
        val q = entityManager.createQuery(
            """
            select count(r) from Reservation r
            where r.status = :st
              and r.visitDate >= :vf
              and r.visitDate <= :vt
            """.trimIndent()
        )
        q.setParameter("st", status)
        q.setParameter("vf", from)
        q.setParameter("vt", to)
        return (q.singleResult as Number).toLong()
    }
}
