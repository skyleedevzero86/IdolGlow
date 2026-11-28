package com.sleekydz86.idolglow.payment.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "내 Glow Pass 이용 통계")
data class MyGlowPassStatsResponse(
    @field:Schema(description = "멤버십 시작일, 결제 완료 회차가 없으면 비어 있음")
    val membershipSince: LocalDate?,
    @field:Schema(description = "결제 완료된 정기결제 회차 금액 합(원)")
    val totalPaid: Long,
    @field:Schema(description = "결제 완료된 정기결제 회차 수")
    val paidCount: Int,
    @field:Schema(description = "예약 회원 할인으로 아낀 금액(원)")
    val savedByDiscount: Long,
    @field:Schema(description = "일반 오픈 전에 확정한 예약 수")
    val earlyBookings: Int,
    @field:Schema(description = "회원 혜택으로 무료가 된 취소 수")
    val freeCancels: Int,
)
