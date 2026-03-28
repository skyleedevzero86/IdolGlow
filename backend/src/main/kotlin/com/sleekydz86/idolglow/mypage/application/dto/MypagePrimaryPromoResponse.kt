package com.sleekydz86.idolglow.mypage.application.dto

import io.swagger.v3.oas.annotations.media.Schema

data class MypagePrimaryPromoResponse(
    @Schema(description = "PICK(추천·인기 상품 기반) | DEFAULT(상품 없음)")
    val variant: String,
    @Schema(description = "강조 이름 앞 문구")
    val textBeforeHighlight: String,
    @Schema(description = "강조 텍스트(상품명 또는 브랜드)")
    val highlight: String,
    @Schema(description = "강조 이름 뒤 문구")
    val textAfterHighlight: String,
    @Schema(description = "프론트 라우트 경로", example = "/articles")
    val href: String,
    @Schema(description = "버튼·링크 라벨")
    val ctaLabel: String,
)
