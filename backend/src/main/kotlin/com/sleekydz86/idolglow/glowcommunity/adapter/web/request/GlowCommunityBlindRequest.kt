package com.sleekydz86.idolglow.glowcommunity.adapter.web.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "관리자 블라인드 요청")
data class GlowCommunityBlindRequest(
    @field:Size(max = 100)
    @field:Schema(description = "블라인드 사유. 비우면 기본 문구가 저장됩니다.", example = "운영 정책 위반")
    val reason: String? = null,
)
