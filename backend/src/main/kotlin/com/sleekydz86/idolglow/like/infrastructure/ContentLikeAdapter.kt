package com.sleekydz86.idolglow.like.infrastructure

import com.sleekydz86.idolglow.like.application.port.out.ContentLikePort
import com.sleekydz86.idolglow.like.domain.ContentLike
import com.sleekydz86.idolglow.like.domain.LikeTarget
import com.sleekydz86.idolglow.like.domain.LikeTargetType
import org.springframework.stereotype.Component

@Component
class ContentLikeAdapter(
    private val contentLikeJpaRepository: ContentLikeJpaRepository,
) : ContentLikePort {
    override fun exists(
        userId: Long,
        target: LikeTarget,
    ): Boolean = contentLikeJpaRepository.existsByUserIdAndTargetTypeAndTargetKey(userId, target.type, target.key)

    override fun save(like: ContentLike) {
        contentLikeJpaRepository.saveAndFlush(like)
    }

    override fun delete(
        userId: Long,
        target: LikeTarget,
    ) {
        contentLikeJpaRepository.deleteByUserAndTarget(userId, target.type, target.key)
    }

    override fun count(target: LikeTarget): Long =
        contentLikeJpaRepository.countByTargetTypeAndTargetKey(target.type, target.key)

    override fun countByKeys(
        type: LikeTargetType,
        keys: Collection<String>,
    ): Map<String, Long> {
        if (keys.isEmpty()) return emptyMap()
        return contentLikeJpaRepository
            .countGroupedByKey(type, keys)
            .associate { row -> row[0].toString() to (row[1] as Number).toLong() }
    }

    override fun findLikedKeys(
        userId: Long,
        type: LikeTargetType,
        keys: Collection<String>,
    ): Set<String> =
        if (keys.isEmpty()) emptySet() else contentLikeJpaRepository.findLikedKeys(userId, type, keys).toSet()
}
