package com.sleekydz86.idolglow.idolglowissue.domain

import java.util.Locale

enum class IdolGlowArticleSort {
    LATEST,
    POPULAR,
    LIKES,
    ;

    companion object {
        fun fromCode(raw: String?): IdolGlowArticleSort {
            val normalized = raw?.trim()?.uppercase(Locale.ROOT).orEmpty()
            if (normalized.isEmpty()) return LATEST
            return entries.firstOrNull { it.name == normalized }
                ?: throw IllegalArgumentException("정렬은 latest, popular, likes 중 하나여야 합니다.")
        }
    }
}
