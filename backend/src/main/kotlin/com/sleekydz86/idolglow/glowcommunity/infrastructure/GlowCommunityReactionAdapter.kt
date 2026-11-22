package com.sleekydz86.idolglow.glowcommunity.infrastructure

import com.sleekydz86.idolglow.glowcommunity.application.port.out.GlowCommunityReactionPort
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityLike
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityReport
import org.springframework.stereotype.Component

@Component
class GlowCommunityReactionAdapter(
    private val glowCommunityLikeJpaRepository: GlowCommunityLikeJpaRepository,
    private val glowCommunityReportJpaRepository: GlowCommunityReportJpaRepository,
) : GlowCommunityReactionPort {
    override fun findLike(
        postId: Long,
        userId: Long,
    ): GlowCommunityLike? = glowCommunityLikeJpaRepository.findByPostIdAndUserId(postId, userId)

    override fun saveLike(like: GlowCommunityLike): GlowCommunityLike = glowCommunityLikeJpaRepository.save(like)

    override fun deleteLike(like: GlowCommunityLike) = glowCommunityLikeJpaRepository.delete(like)

    override fun findLikedPostIds(
        userId: Long,
        postIds: Collection<Long>,
    ): Set<Long> =
        if (postIds.isEmpty()) emptySet() else glowCommunityLikeJpaRepository.findLikedPostIds(userId, postIds).toSet()

    override fun existsReport(
        postId: Long,
        userId: Long,
    ): Boolean = glowCommunityReportJpaRepository.existsByPostIdAndUserId(postId, userId)

    override fun saveReport(report: GlowCommunityReport): GlowCommunityReport =
        glowCommunityReportJpaRepository.save(report)

    override fun findReportsByPostIds(postIds: Collection<Long>): List<GlowCommunityReport> =
        if (postIds.isEmpty()) emptyList() else glowCommunityReportJpaRepository.findAllByPostIdIn(postIds)
}
