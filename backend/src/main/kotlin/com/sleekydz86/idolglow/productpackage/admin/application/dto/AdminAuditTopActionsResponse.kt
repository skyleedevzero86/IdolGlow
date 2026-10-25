package com.sleekydz86.idolglow.productpackage.admin.application.dto

import io.swagger.v3.oas.annotations.media.Schema

data class AdminAuditTopActionRow(
    @field:Schema(description = "순위")
    val rank: Int,
    @field:Schema(description = "액션 코드")
    val actionCode: String,
    @field:Schema(description = "반복 횟수")
    val count: Long,
)

data class AdminAuditTopActionsResponse(
    @field:Schema(description = "집계 기준. 로컬이면 로컬 데이터 기준")
    val basis: String,
    @field:Schema(description = "가장 많이 반복된 관리자 작업")
    val actions: List<AdminAuditTopActionRow>,
    @field:Schema(description = "자동화 후보 액션 코드")
    val automationActionCode: String?,
    @field:Schema(description = "자동화 후보를 고른 이유")
    val automationReason: String,
)
