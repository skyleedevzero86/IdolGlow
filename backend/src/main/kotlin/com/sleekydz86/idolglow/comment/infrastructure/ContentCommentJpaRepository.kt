package com.sleekydz86.idolglow.comment.infrastructure

import com.sleekydz86.idolglow.comment.domain.ContentComment
import com.sleekydz86.idolglow.global.support.ContentTargetType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ContentCommentJpaRepository : JpaRepository<ContentComment, Long> {
    @Query(
        value = """
            select c from ContentComment c
            where c.targetType = :targetType
              and c.targetKey = :targetKey
              and c.parentId is null
              and (c.deletedAt is null
                or exists (select r.id from ContentComment r where r.rootId = c.id and r.deletedAt is null))
        """,
        countQuery = """
            select count(c) from ContentComment c
            where c.targetType = :targetType
              and c.targetKey = :targetKey
              and c.parentId is null
              and (c.deletedAt is null
                or exists (select r.id from ContentComment r where r.rootId = c.id and r.deletedAt is null))
        """,
    )
    fun findThreadRoots(
        @Param("targetType") targetType: ContentTargetType,
        @Param("targetKey") targetKey: String,
        pageable: Pageable,
    ): Page<ContentComment>

    @Query("select c from ContentComment c where c.rootId in :rootIds")
    fun findAllByRootIdIn(
        @Param("rootIds") rootIds: Collection<Long>,
    ): List<ContentComment>

    @Query(
        value = "select c from ContentComment c where c.userId = :userId and c.deletedAt is null",
        countQuery = "select count(c) from ContentComment c where c.userId = :userId and c.deletedAt is null",
    )
    fun findActiveByUserId(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<ContentComment>
}
