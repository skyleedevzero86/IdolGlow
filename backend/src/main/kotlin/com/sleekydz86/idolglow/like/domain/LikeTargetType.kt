package com.sleekydz86.idolglow.like.domain

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.like.LikeExceptionType
import java.util.Locale

enum class LikeTargetType {
    ARTICLE,
    EVENT_INFO,
    PRODUCT_REVIEW,
    CONTENT_COMMENT,
    GLOW_COMMUNITY_POST,
    ;

    companion object {
        fun parse(raw: String?): LikeTargetType {
            val normalized =
                raw
                    ?.trim()
                    ?.uppercase(Locale.ROOT)
                    ?.replace('-', '_')
                    .orEmpty()
            return entries.firstOrNull { it.name == normalized }
                ?: throw CustomException(LikeExceptionType.INVALID_TARGET_TYPE)
        }
    }
}
