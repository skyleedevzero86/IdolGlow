package com.sleekydz86.idolglow.like.application.port.out

import com.sleekydz86.idolglow.like.domain.ContentLike
import com.sleekydz86.idolglow.like.domain.LikeTarget
import com.sleekydz86.idolglow.like.domain.LikeTargetType

interface ContentLikePort {
    fun exists(
        userId: Long,
        target: LikeTarget,
    ): Boolean

    fun save(like: ContentLike)

    fun delete(
        userId: Long,
        target: LikeTarget,
    )

    fun count(target: LikeTarget): Long

    fun countByKeys(
        type: LikeTargetType,
        keys: Collection<String>,
    ): Map<String, Long>

    fun findLikedKeys(
        userId: Long,
        type: LikeTargetType,
        keys: Collection<String>,
    ): Set<String>
}
