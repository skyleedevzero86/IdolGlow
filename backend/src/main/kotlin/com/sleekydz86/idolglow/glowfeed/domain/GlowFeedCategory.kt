package com.sleekydz86.idolglow.glowfeed.domain

import java.util.Locale

enum class GlowFeedCategory(
    val code: String,
) {
    TREND("trend"),
    NEWS("news"),
    RECENT("recent"),
    LOCAL("local"),
    ;

    companion object {
        private const val ALL = "all"

        fun fromFilter(raw: String?): GlowFeedCategory? {
            val normalized = raw?.trim()?.lowercase(Locale.ROOT).orEmpty()
            if (normalized.isEmpty() || normalized == ALL) return null
            return entries.firstOrNull { it.code == normalized || it.name.lowercase(Locale.ROOT) == normalized }
                ?: throw IllegalArgumentException("카테고리는 all, trend, news, recent, local 중 하나여야 합니다.")
        }
    }
}
