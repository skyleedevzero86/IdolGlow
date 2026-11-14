package com.sleekydz86.idolglow.glowfeed.domain

import java.util.Locale

enum class GlowFeedHighlightSort {
    LATEST,
    POPULAR,
    ;

    companion object {
        fun fromCode(raw: String?): GlowFeedHighlightSort {
            val normalized = raw?.trim()?.uppercase(Locale.ROOT).orEmpty()
            if (normalized.isEmpty()) return LATEST
            return entries.firstOrNull { it.name == normalized }
                ?: throw IllegalArgumentException("정렬은 latest 또는 popular 여야 합니다.")
        }
    }
}
