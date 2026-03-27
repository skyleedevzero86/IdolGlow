package com.sleekydz86.idolglow.productpackage.reservation.application.dto

import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationCancelReason
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(description = "예약 요약 응답 DTO")
data class ReservationSummaryResponse(
    @field:Schema(description = "예약 ID", example = "1")
    val reservationId: Long,
    @field:Schema(description = "예약 상태", example = "PENDING")
    val status: ReservationStatus,
    @field:Schema(description = "상품 ID", example = "12")
    val productId: Long,
    @field:Schema(description = "상품명", example = "헤어 메이크업 패키지")
    val productName: String,
    @field:Schema(description = "상품 설명", example = "헤어와 메이크업이 포함된 상품입니다.")
    val productDescription: String,
    @field:Schema(description = "총액", example = "300000.00")
    val totalPrice: BigDecimal,
    @field:Schema(description = "방문 날짜", example = "2026-03-21")
    val visitDate: LocalDate,
    @field:Schema(description = "방문 시작 시각", example = "09:00:00")
    val visitStartTime: LocalTime,
    @field:Schema(description = "방문 종료 시각", example = "10:00:00")
    val visitEndTime: LocalTime,
    @field:Schema(description = "선택된 구성 항목", example = "[\"헤어\", \"메이크업\"]")
    val attractions: List<String>,
    @field:Schema(description = "예약 만료 시각", example = "2026-03-21T12:15:00")
    val expiresAt: LocalDateTime?,
    @field:Schema(description = "예약 확정 시각", example = "2026-03-21T12:05:00")
    val confirmedAt: LocalDateTime?,
    @field:Schema(description = "예약 취소 시각", example = "2026-03-21T12:06:00")
    val canceledAt: LocalDateTime?,
    @field:Schema(description = "취소 사유", example = "PAYMENT_FAILED")
    val cancelReason: ReservationCancelReason?,
) {
    companion object {
        fun from(
            reservation: Reservation,
            status: ReservationStatus
        ): ReservationSummaryResponse {
            val product = reservation.reservationSlot.product
            val attractions = product.productOptions.map { it.option.name }.distinct()
            return ReservationSummaryResponse(
                reservationId = reservation.id,
                status = status,
                productId = product.id,
                productName = product.name,
                productDescription = product.description,
                totalPrice = reservation.totalPrice,
                visitDate = reservation.visitDate,
                visitStartTime = reservation.visitStartTime,
                visitEndTime = reservation.visitEndTime,
                attractions = attractions,
                expiresAt = reservation.expiresAt,
                confirmedAt = reservation.confirmedAt,
                canceledAt = reservation.canceledAt,
                cancelReason = reservation.cancelReason
            )
        }
    }
}
