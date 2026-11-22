package com.sleekydz86.idolglow.glowcommunity.infrastructure

import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityPost
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GlowCommunityPostJpaRepository :
    JpaRepository<GlowCommunityPost, Long>,
    JpaSpecificationExecutor<GlowCommunityPost> {
    @Query("select p from GlowCommunityPost p where p.id = :postId and p.deletedAt is null")
    fun findActiveById(
        @Param("postId") postId: Long,
    ): GlowCommunityPost?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from GlowCommunityPost p where p.id = :postId and p.deletedAt is null")
    fun findActiveByIdForUpdate(
        @Param("postId") postId: Long,
    ): GlowCommunityPost?

    @Query(
        """
        select p from GlowCommunityPost p
        where p.deletedAt is null
          and p.blinded = false
          and (p.likeCount > 0 or p.commentCount > 0)
        order by
          case when p.likeCount >= p.commentCount then p.likeCount else p.commentCount end desc,
          case when p.likeCount >= p.commentCount then p.commentCount else p.likeCount end desc,
          p.createdAt desc,
          p.id desc
        """,
    )
    fun findHighlights(pageable: Pageable): List<GlowCommunityPost>

    @Query(
        value = "select p from GlowCommunityPost p where p.userId = :userId and p.deletedAt is null",
        countQuery = "select count(p) from GlowCommunityPost p where p.userId = :userId and p.deletedAt is null",
    )
    fun findActiveByUserId(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<GlowCommunityPost>

    @Query(
        value = "select p from GlowCommunityPost p where p.reportCount > 0 and p.deletedAt is null",
        countQuery = "select count(p) from GlowCommunityPost p where p.reportCount > 0 and p.deletedAt is null",
    )
    fun findReported(pageable: Pageable): Page<GlowCommunityPost>

    @Query("select p from GlowCommunityPost p where p.deletedAt is null and p.blinded = false")
    fun findVisible(pageable: Pageable): List<GlowCommunityPost>

    @Query(
        """
        select p from GlowCommunityPost p
        where p.deletedAt is null and p.blinded = false
        order by (p.likeCount + p.commentCount) desc, p.createdAt desc, p.id desc
        """,
    )
    fun findPopularVisible(pageable: Pageable): List<GlowCommunityPost>
}
