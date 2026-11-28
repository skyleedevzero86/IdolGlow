package com.sleekydz86.idolglow.payment.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "바로 취소 결과")
data class QuickCancelResponse(
    @field:Schema(description = "취소한 예약 번호")
    val reservationId: Long,
)
