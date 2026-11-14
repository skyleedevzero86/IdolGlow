package com.sleekydz86.idolglow.glowcommunity.adapter.web.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "지역사회 게시글 신고 요청")
data class GlowCommunityReportRequest(
    @field:Size(max = 200)
    @field:Schema(description = "신고 사유. 비워도 됩니다.", example = "광고성 글입니다")
    val reason: String? = null,
)
