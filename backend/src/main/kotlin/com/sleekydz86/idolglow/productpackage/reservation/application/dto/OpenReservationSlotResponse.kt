package com.sleekydz86.idolglow.productpackage.reservation.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(description = "회원이 고를 수 있는 예약 슬롯")
data class OpenReservationSlotResponse(
    val reservationSlotId: Long,
    val reservationDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    @field:Schema(description = "지금 예약할 수 있는지. 예약됐거나 다른 회원이 결제 중이면 false")
    val available: Boolean,
    @field:Schema(description = "Glow Pass 회원 오픈 시각")
    val memberOpenAt: LocalDateTime,
    @field:Schema(description = "일반 오픈 시각")
    val publicOpenAt: LocalDateTime,
    @field:Schema(description = "로그인한 회원이 낼 금액 원 단위")
    val totalPrice: BigDecimal,
)
