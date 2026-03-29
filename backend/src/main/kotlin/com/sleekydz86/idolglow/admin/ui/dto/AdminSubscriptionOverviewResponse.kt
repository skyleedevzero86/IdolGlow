package com.sleekydz86.idolglow.admin.ui.dto

import io.swagger.v3.oas.annotations.media.Schema

data class AdminSubscriptionOverviewResponse(
    @Schema(description = "활성 구독 수(현재 미연동 시 0)")
    val totalActive: Long = 0,
)
