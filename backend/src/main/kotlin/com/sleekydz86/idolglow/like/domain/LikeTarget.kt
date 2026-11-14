package com.sleekydz86.idolglow.like.domain

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.like.LikeExceptionType

data class LikeTarget(
    val type: LikeTargetType,
    val key: String,
) {
    companion object {
        const val KEY_MAX_LENGTH = 200

        fun of(
            rawType: String?,
            rawKey: String?,
        ): LikeTarget {
            val type = LikeTargetType.parse(rawType)
            val key = rawKey?.trim().orEmpty()
            if (key.isEmpty() || key.length > KEY_MAX_LENGTH) {
                throw CustomException(LikeExceptionType.INVALID_TARGET_KEY)
            }
            return LikeTarget(type, key)
        }
    }
}
