package com.sleekydz86.idolglow.like.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.like.LikeExceptionType
import com.sleekydz86.idolglow.like.application.dto.ContentLikeResult
import com.sleekydz86.idolglow.like.application.dto.LikeSummary
import com.sleekydz86.idolglow.like.application.port.`in`.ContentLikeQueryUseCase
import com.sleekydz86.idolglow.like.application.port.`in`.ContentLikeUseCase
import com.sleekydz86.idolglow.like.application.port.out.ContentLikePort
import com.sleekydz86.idolglow.like.application.port.out.DelegatedLikePort
import com.sleekydz86.idolglow.like.application.port.out.LikeTargetPort
import com.sleekydz86.idolglow.like.domain.ContentLike
import com.sleekydz86.idolglow.like.domain.LikeTarget
import com.sleekydz86.idolglow.like.domain.LikeTargetType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ContentLikeService(
    private val contentLikePort: ContentLikePort,
    private val likeTargetPort: LikeTargetPort,
    delegatedLikePorts: List<DelegatedLikePort>,
) : ContentLikeUseCase,
    ContentLikeQueryUseCase {
    private val delegates: Map<LikeTargetType, DelegatedLikePort> = delegatedLikePorts.associateBy { it.targetType }

    override fun like(
        userId: Long,
        target: LikeTarget,
    ): ContentLikeResult {
        delegates[target.type]?.let { return it.like(target.key, userId) }
        if (!likeTargetPort.isLikeable(target)) throw CustomException(LikeExceptionType.TARGET_NOT_FOUND)
        if (!contentLikePort.exists(userId, target)) contentLikePort.save(ContentLike.of(userId, target))
        return ContentLikeResult(liked = true, likeCount = contentLikePort.count(target))
    }

    override fun unlike(
        userId: Long,
        target: LikeTarget,
    ): ContentLikeResult {
        delegates[target.type]?.let { return it.unlike(target.key, userId) }
        contentLikePort.delete(userId, target)
        return ContentLikeResult(liked = false, likeCount = contentLikePort.count(target))
    }

    @Transactional(readOnly = true)
    override fun summarize(
        targets: Collection<LikeTarget>,
        viewerId: Long?,
    ): Map<LikeTarget, LikeSummary> =
        targets
            .distinct()
            .groupBy { it.type }
            .flatMap { (type, grouped) -> summarizeType(type, grouped.map { it.key }, viewerId).toList() }
            .toMap()

    private fun summarizeType(
        type: LikeTargetType,
        keys: List<String>,
        viewerId: Long?,
    ): Map<LikeTarget, LikeSummary> {
        val delegate = delegates[type]
        if (delegate != null) {
            val summaries = delegate.summarize(keys, viewerId)
            return keys.associate { key -> LikeTarget(type, key) to (summaries[key] ?: LikeSummary.EMPTY) }
        }
        val counts = contentLikePort.countByKeys(type, keys)
        val liked = viewerId?.let { contentLikePort.findLikedKeys(it, type, keys) }.orEmpty()
        return keys.associate { key ->
            LikeTarget(type, key) to LikeSummary(likeCount = counts[key] ?: 0L, liked = key in liked)
        }
    }
}
