package com.sleekydz86.idolglow.mypage.application.dto

import io.swagger.v3.oas.annotations.media.Schema

data class MypageSecondaryPromoResponse(
    @Schema(description = "UPCOMING | HISTORY | EMPTY")
    val variant: String,
    @Schema(description = "<strong> 앞 문구")
    val textBeforeStrong: String,
    @Schema(description = "강조 문구")
    val strong: String,
    @Schema(description = "<strong> 뒤 문구")
    val textAfterStrong: String,
    @Schema(description = "표시용 숫자(최대 99)")
    val metricValue: Int,
    @Schema(description = "숫자 옆 단위 문구")
    val metricUnit: String,
    @Schema(description = "프론트 라우트(해시 앵커 가능)", example = "/mypage#upcoming-strip")
    val href: String,
)
