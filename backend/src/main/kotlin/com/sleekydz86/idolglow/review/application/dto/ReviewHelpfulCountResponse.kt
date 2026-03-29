package com.sleekydz86.idolglow.review.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "도움돼요 토글 후 카운트")
data class ReviewHelpfulCountResponse(
    @field:Schema(description = "도움돼요 수")
    val helpfulCount: Long,
)
