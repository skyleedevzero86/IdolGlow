package com.sleekydz86.idolglow.glowcommunity.application.dto

import java.time.LocalDateTime

data class GlowCommunityPostResult(
    val id: Long,
    val tag: String,
    val tagLabel: String,
    val title: String,
    val content: String,
    val authorName: String,
    val authorId: Long?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val likeCount: Long,
    val commentCount: Long,
    val liked: Boolean,
    val likedByMe: Boolean,
    val mine: Boolean,
    val blinded: Boolean,
    val blindReason: String?,
)

data class GlowCommunityLikeResult(
    val liked: Boolean,
    val likeCount: Long,
)

data class GlowCommunityReportReasonResult(
    val reason: String?,
    val reportedAt: LocalDateTime?,
)

data class GlowCommunityReportedPostResult(
    val postId: Long,
    val tag: String,
    val title: String,
    val authorId: Long,
    val authorName: String,
    val reportCount: Long,
    val lastReportedAt: LocalDateTime?,
    val blinded: Boolean,
    val blindedAt: LocalDateTime?,
    val blindReason: String?,
    val createdAt: LocalDateTime?,
    val reports: List<GlowCommunityReportReasonResult>,
)

data class GlowCommunityKeywordsResult(
    val keywords: List<String>,
    val source: String,
    val generatedAt: LocalDateTime?,
) {
    companion object {
        const val SOURCE_LLM = "llm"
        const val SOURCE_FALLBACK = "fallback"
    }
}

data class GlowCommunityKeywordSource(
    val title: String,
    val snippet: String,
)
