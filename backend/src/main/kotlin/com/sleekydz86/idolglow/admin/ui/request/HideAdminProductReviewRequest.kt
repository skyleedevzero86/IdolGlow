package com.sleekydz86.idolglow.admin.ui.request

import io.swagger.v3.oas.annotations.media.Schema

data class HideAdminProductReviewRequest(
    @field:Schema(description = "비공개 사유(최대 80자). 비우면 기본 문구가 저장됩니다.", maxLength = 80)
    val reason: String? = null,
)
