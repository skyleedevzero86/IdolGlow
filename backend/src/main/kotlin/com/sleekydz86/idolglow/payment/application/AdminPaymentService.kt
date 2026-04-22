package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentDetailResponse
import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentChartsResponse
import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentLogResponse
import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentOverviewResponse
import com.sleekydz86.idolglow.payment.application.dto.PaymentMonthlyChartPoint
import com.sleekydz86.idolglow.payment.application.dto.PaymentStatusChartPoint
import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentSummaryResponse
import com.sleekydz86.idolglow.payment.application.dto.PaymentRefundResponse
import com.sleekydz86.idolglow.payment.infrastructure.PaymentLogJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.PaymentRefundJpaRepository
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.AdminPaymentQueryRepository
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Transactional(readOnly = true)
@Service
class AdminPaymentService(
    private val adminPaymentQueryRepository: AdminPaymentQueryRepository,
    private val paymentRefundJpaRepository: PaymentRefundJpaRepository,
    private val paymentLogJpaRepository: PaymentLogJpaRepository,
    private val reservationCommandService: ReservationCommandService,
    private val paymentViewPolicyService: PaymentViewPolicyService,
    private val paymentDocumentService: PaymentDocumentService,
) {

    fun findPayments(
        status: PaymentStatus?,
        visitDate: LocalDate?,
        productId: Long?,
        size: Int,
    ): List<AdminPaymentSummaryResponse> =
        adminPaymentQueryRepository.findPayments(
            status = status,
            visitDate = visitDate,
            productId = productId,
            size = size,
        ).map(AdminPaymentSummaryResponse::from)

    fun overview(
        status: PaymentStatus?,
        visitDate: LocalDate?,
        productId: Long?,
    ): AdminPaymentOverviewResponse {
        val items = adminPaymentQueryRepository.findPayments(
            status = status,
            visitDate = visitDate,
            productId = productId,
            size = 5_000,
        )

        val grossAmount = items.fold(BigDecimal.ZERO) { acc, payment -> acc + payment.amount }
        val refundedAmount = items.fold(BigDecimal.ZERO) { acc, payment -> acc + payment.cancelAmount }

        return AdminPaymentOverviewResponse(
            totalCount = items.size.toLong(),
            pendingCount = items.count { it.status == PaymentStatus.PENDING }.toLong(),
            succeededCount = items.count { it.status == PaymentStatus.SUCCEEDED }.toLong(),
            failedCount = items.count { it.status == PaymentStatus.FAILED }.toLong(),
            canceledCount = items.count { it.status == PaymentStatus.CANCELED }.toLong(),
            expiredCount = items.count { it.status == PaymentStatus.EXPIRED }.toLong(),
            refundedCount = items.count { it.status == PaymentStatus.REFUNDED }.toLong(),
            partialCanceledCount = items.count { it.status == PaymentStatus.PARTIAL_CANCELED }.toLong(),
            cancelableCount = items.count(paymentViewPolicyService::canAdminCancel).toLong(),
            grossAmount = grossAmount,
            refundedAmount = refundedAmount,
            netAmount = grossAmount.subtract(refundedAmount),
        )
    }

    fun charts(
        status: PaymentStatus?,
        visitDate: LocalDate?,
        productId: Long?,
    ): AdminPaymentChartsResponse {
        val items = adminPaymentQueryRepository.findPayments(
            status = status,
            visitDate = visitDate,
            productId = productId,
            size = 5_000,
        )
        val byStatus = items
            .groupBy { it.status.name }
            .toSortedMap()
            .map { (key, list) -> PaymentStatusChartPoint(status = key, count = list.size.toLong()) }

        val byMonth = items
            .groupBy { it.reservation.visitDate.withDayOfMonth(1) }
            .toSortedMap()
            .map { (month, list) ->
                PaymentMonthlyChartPoint(
                    month = month.format(MONTH_FORMAT),
                    totalCount = list.size.toLong(),
                    succeededCount = list.count { it.status == PaymentStatus.SUCCEEDED }.toLong(),
                    failedCount = list.count { it.status == PaymentStatus.FAILED || it.status == PaymentStatus.EXPIRED }.toLong(),
                    canceledCount = list.count {
                        it.status == PaymentStatus.CANCELED ||
                            it.status == PaymentStatus.REFUNDED ||
                            it.status == PaymentStatus.PARTIAL_CANCELED
                    }.toLong(),
                )
            }

        return AdminPaymentChartsResponse(byStatus = byStatus, byMonth = byMonth)
    }

    fun findPaymentDetail(paymentId: Long): AdminPaymentDetailResponse {
        val payment = adminPaymentQueryRepository.findPaymentById(paymentId)
            ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
        val refunds = paymentRefundJpaRepository.findAllByPaymentIdOrderByCreatedAtDesc(paymentId)
            .map(PaymentRefundResponse::from)
        val logs = paymentLogJpaRepository.findAllByPaymentIdOrderByCreatedAtDesc(paymentId)
            .map(AdminPaymentLogResponse::from)
        return AdminPaymentDetailResponse.from(
            payment = payment,
            canCancel = paymentViewPolicyService.canAdminCancel(payment),
            receiptAvailable = paymentViewPolicyService.receiptAvailable(payment),
            refunds = refunds,
            logs = logs,
        )
    }

    @Transactional
    fun cancelPayment(paymentId: Long, reason: String? = null): AdminPaymentDetailResponse {
        val payment = adminPaymentQueryRepository.findPaymentById(paymentId)
            ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
        require(paymentViewPolicyService.canAdminCancel(payment)) { "관리자 취소가 가능한 결제 상태가 아닙니다." }
        reservationCommandService.cancelReservationByAdmin(payment.reservation.id, reason)
        return findPaymentDetail(paymentId)
    }

    fun exportPaymentsXlsx(
        status: PaymentStatus?,
        visitDate: LocalDate?,
        productId: Long?,
    ): ByteArray =
        paymentDocumentService.exportPaymentsXlsx(
            adminPaymentQueryRepository.findPayments(
                status = status,
                visitDate = visitDate,
                productId = productId,
                size = 5_000,
            )
        )

    fun renderReceiptPdf(paymentId: Long): ByteArray {
        val payment = adminPaymentQueryRepository.findPaymentById(paymentId)
            ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
        require(paymentViewPolicyService.receiptAvailable(payment)) { "영수증을 출력할 수 없는 결제 상태입니다." }
        return paymentDocumentService.buildReceiptPdf(payment, "관리자")
    }

    companion object {
        private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    }
}
