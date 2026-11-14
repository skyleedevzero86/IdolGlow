package com.sleekydz86.idolglow.glowfeed.application.dto

import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedKind
import com.sleekydz86.idolglow.like.domain.LikeTarget
import java.time.LocalDateTime

data class GlowFeedEntry(
    val kind: GlowFeedKind,
    val refId: Long,
    val sortAt: LocalDateTime,
    val likeKey: String,
    val viewTarget: GlowFeedViewTarget,
) {
    val likeTarget: LikeTarget
        get() = LikeTarget(kind.likeTargetType, likeKey)
}

data class GlowFeedViewTarget(
    val contentType: String,
    val contentKey: String,
)

data class GlowFeedContent(
    val title: String,
    val imageUrl: String?,
    val path: String,
    val volume: Int? = null,
    val meta: String? = null,
    val tags: List<String> = emptyList(),
)
