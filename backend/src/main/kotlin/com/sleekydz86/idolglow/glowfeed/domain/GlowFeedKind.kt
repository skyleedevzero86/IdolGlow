package com.sleekydz86.idolglow.glowfeed.domain

import com.sleekydz86.idolglow.like.domain.LikeTargetType

enum class GlowFeedKind(
    val category: GlowFeedCategory,
    val likeTargetType: LikeTargetType,
) {
    ARTICLE(GlowFeedCategory.TREND, LikeTargetType.ARTICLE),
    EVENT(GlowFeedCategory.NEWS, LikeTargetType.EVENT_INFO),
    PRODUCT_REVIEW(GlowFeedCategory.RECENT, LikeTargetType.PRODUCT_REVIEW),
    COMMENT(GlowFeedCategory.RECENT, LikeTargetType.CONTENT_COMMENT),
    COMMUNITY_POST(GlowFeedCategory.LOCAL, LikeTargetType.GLOW_COMMUNITY_POST),
    ;

    companion object {
        fun of(category: GlowFeedCategory?): List<GlowFeedKind> =
            if (category == null) entries else entries.filter { it.category == category }
    }
}
