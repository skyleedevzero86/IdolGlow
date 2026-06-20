package com.sleekydz86.idolglow.productpackage.admin.application.dto

import io.swagger.v3.oas.annotations.media.Schema

data class CancelReasonStatRow(
    @field:Schema(description = "취소 사유 코드 널이면 미기록")
    val reason: String?,
    val count: Long,
)
