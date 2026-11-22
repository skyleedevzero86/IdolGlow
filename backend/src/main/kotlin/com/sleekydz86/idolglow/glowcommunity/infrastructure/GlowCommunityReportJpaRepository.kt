package com.sleekydz86.idolglow.glowcommunity.infrastructure

import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityReport
import org.springframework.data.jpa.repository.JpaRepository

interface GlowCommunityReportJpaRepository : JpaRepository<GlowCommunityReport, Long> {
    fun existsByPostIdAndUserId(
        postId: Long,
        userId: Long,
    ): Boolean

    fun findAllByPostIdIn(postIds: Collection<Long>): List<GlowCommunityReport>
}
