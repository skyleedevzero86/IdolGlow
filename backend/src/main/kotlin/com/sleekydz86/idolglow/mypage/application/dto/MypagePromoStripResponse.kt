package com.sleekydz86.idolglow.mypage.application.dto

import io.swagger.v3.oas.annotations.media.Schema

data class MypagePromoStripResponse(
    @Schema(description = "첫 번째 프로모(추천 패키지)")
    val primary: MypagePrimaryPromoResponse,
    @Schema(description = "두 번째 프로모(예약·일정)")
    val secondary: MypageSecondaryPromoResponse,
)
