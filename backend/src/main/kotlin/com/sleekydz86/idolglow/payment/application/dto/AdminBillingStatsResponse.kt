package com.sleekydz86.idolglow.payment.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관리자 Glow Pass 구독 통계")
data class AdminBillingStatsResponse(
    @field:Schema(description = "자동 연장 중인 구독 수")
    val activeCount: Long,
    @field:Schema(description = "이번 기간만 이용하는 구독 수")
    val cancelScheduledCount: Long,
    @field:Schema(description = "일시정지된 구독 수")
    val pausedCount: Long,
    @field:Schema(description = "결제 재시도 중인 구독 수")
    val retryingCount: Long,
    @field:Schema(description = "결제 주기별 구독자 수")
    val byCycle: List<BillingCycleCount>,
    @field:Schema(description = "환산 월 매출(원), 주기별 금액을 개월 수로 나눈 합")
    val monthlyRevenue: Long,
    @field:Schema(description = "최근 7일 갱신 안내 메일 응답")
    val renewalNotice: RenewalNoticeStats,
)

data class BillingCycleCount(
    val cycleMonths: Int,
    val count: Long,
)

data class RenewalNoticeStats(
    val sent: Long,
    val continued: Long,
    val stopped: Long,
    val noAnswer: Long,
)
