package com.sleekydz86.idolglow.glowcommunity.domain

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.glowcommunity.GlowCommunityExceptionType

enum class GlowCommunityTag(
    val code: String,
    val label: String,
) {
    TALK("talk", "토크"),
    QUESTION("question", "질문"),
    TRAVEL("travel", "나의 여행 계획"),
    ;

    private fun matches(value: String): Boolean =
        code.equals(value, ignoreCase = true) || name.equals(value, ignoreCase = true)

    companion object {
        fun fromCode(raw: String): GlowCommunityTag {
            val normalized = raw.trim()
            return entries.firstOrNull { it.matches(normalized) }
                ?: throw CustomException(GlowCommunityExceptionType.INVALID_TAG)
        }

        fun fromFilter(raw: String?): GlowCommunityTag? {
            val normalized = raw?.trim().orEmpty()
            return if (normalized.isEmpty() || normalized.equals(ALL, ignoreCase = true)) null else fromCode(normalized)
        }

        private const val ALL = "all"
    }
}
