package com.sleekydz86.idolglow.productpackage.admin.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

data class CancellationComparisonResponse(
    val current: PeriodCancellationMetrics,
    val previous: PeriodCancellationMetrics,
    @field:Schema(description = "이전 구간 대비 현재 구간 취소율 차이")
    val cancelRateDelta: BigDecimal?,
)
