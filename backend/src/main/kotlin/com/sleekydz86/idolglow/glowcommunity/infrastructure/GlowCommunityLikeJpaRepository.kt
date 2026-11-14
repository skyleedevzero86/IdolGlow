package com.sleekydz86.idolglow.glowcommunity.infrastructure

import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GlowCommunityLikeJpaRepository : JpaRepository<GlowCommunityLike, Long> {
    fun findByPostIdAndUserId(
        postId: Long,
        userId: Long,
    ): GlowCommunityLike?

    @Query("select l.postId from GlowCommunityLike l where l.userId = :userId and l.postId in :postIds")
    fun findLikedPostIds(
        @Param("userId") userId: Long,
        @Param("postIds") postIds: Collection<Long>,
    ): List<Long>
}
