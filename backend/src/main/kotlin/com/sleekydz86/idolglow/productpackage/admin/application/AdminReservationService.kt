package com.sleekydz86.idolglow.productpackage.admin.application

import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminReservationSummaryResponse
import com.sleekydz86.idolglow.productpackage.admin.application.dto.ReservationDashboardResponse
import com.sleekydz86.idolglow.productpackage.admin.infrastructure.AdminReservationQueryRepository
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class AdminReservationService(
    private val adminReservationQueryRepository: AdminReservationQueryRepository,
    private val reservationCommandService: ReservationCommandService,
) {

    fun findDashboard(
        fromDate: LocalDate?,
        toDate: LocalDate?,
        recentSize: Int,
    ): ReservationDashboardResponse {
        val reservationCounts = adminReservationQueryRepository.countReservationDashboard(LocalDate.now(), fromDate, toDate)
        val paymentCounts = adminReservationQueryRepository.countPaymentsByStatus(fromDate, toDate)
        val recentReservationEntities = adminReservationQueryRepository.findRecentReservations(fromDate, toDate, recentSize)
        val recentReservationPayments = adminReservationQueryRepository.findPaymentsByReservationIds(
            recentReservationEntities.map { it.id }
        )
        val recentReservations = recentReservationEntities.map { reservation ->
            AdminReservationSummaryResponse.from(reservation, recentReservationPayments[reservation.id])
        }

        return ReservationDashboardResponse(
            pendingCount = reservationCounts.pendingCount,
            bookedCount = reservationCounts.bookedCount,
            completedCount = reservationCounts.completedCount,
            canceledCount = reservationCounts.canceledCount,
            paymentPendingCount = paymentCounts[PaymentStatus.PENDING] ?: 0L,
            paymentSucceededCount = paymentCounts[PaymentStatus.SUCCEEDED] ?: 0L,
            paymentFailedCount = paymentCounts[PaymentStatus.FAILED] ?: 0L,
            paymentCanceledCount = paymentCounts[PaymentStatus.CANCELED] ?: 0L,
            paymentExpiredCount = paymentCounts[PaymentStatus.EXPIRED] ?: 0L,
            recentReservations = recentReservations
        )
    }

    fun findReservations(
        status: ReservationStatus?,
        visitDate: LocalDate?,
        productId: Long?,
        size: Int,
    ): List<AdminReservationSummaryResponse> {
        val reservations = adminReservationQueryRepository.findReservations(status, visitDate, productId, size)
        val payments = adminReservationQueryRepository.findPaymentsByReservationIds(reservations.map { it.id })
        return reservations.map { reservation ->
            AdminReservationSummaryResponse.from(reservation, payments[reservation.id])
        }
    }

    @Transactional
    fun cancelReservation(reservationId: Long): AdminReservationSummaryResponse {
        val reservation = reservationCommandService.cancelReservationByAdmin(reservationId)
        val payments = adminReservationQueryRepository.findPaymentsByReservationIds(listOf(reservation.id))
        return AdminReservationSummaryResponse.from(reservation, payments[reservation.id])
    }
}
