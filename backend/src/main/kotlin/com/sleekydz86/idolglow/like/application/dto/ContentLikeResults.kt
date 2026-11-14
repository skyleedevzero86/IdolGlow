package com.sleekydz86.idolglow.like.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "좋아요 결과")
data class ContentLikeResult(
    @field:Schema(description = "요청한 회원이 좋아요를 누른 상태인지", example = "true")
    val liked: Boolean,
    @field:Schema(description = "콘텐츠의 전체 좋아요 수", example = "12")
    val likeCount: Long,
)

data class LikeSummary(
    val likeCount: Long,
    val liked: Boolean,
) {
    companion object {
        val EMPTY = LikeSummary(likeCount = 0L, liked = false)
    }
}
