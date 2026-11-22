package com.sleekydz86.idolglow.glowcommunity.application.port.out

import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityLike
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityReport

interface GlowCommunityReactionPort {
    fun findLike(
        postId: Long,
        userId: Long,
    ): GlowCommunityLike?

    fun saveLike(like: GlowCommunityLike): GlowCommunityLike

    fun deleteLike(like: GlowCommunityLike)

    fun findLikedPostIds(
        userId: Long,
        postIds: Collection<Long>,
    ): Set<Long>

    fun existsReport(
        postId: Long,
        userId: Long,
    ): Boolean

    fun saveReport(report: GlowCommunityReport): GlowCommunityReport

    fun findReportsByPostIds(postIds: Collection<Long>): List<GlowCommunityReport>
}
