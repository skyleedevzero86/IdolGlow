package com.sleekydz86.idolglow.glowcommunity.domain

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.glowcommunity.GlowCommunityExceptionType

enum class GlowCommunitySort(
    val code: String,
) {
    LATEST("latest"),
    POPULAR("popular"),
    ;

    companion object {
        fun fromCode(raw: String?): GlowCommunitySort {
            val normalized = raw?.trim().orEmpty()
            if (normalized.isEmpty()) return LATEST
            return entries.firstOrNull { it.code.equals(normalized, ignoreCase = true) }
                ?: throw CustomException(GlowCommunityExceptionType.INVALID_SORT)
        }
    }
}
