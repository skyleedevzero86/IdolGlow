package com.sleekydz86.idolglow.history.infrastructure

import com.sleekydz86.idolglow.global.support.ContentTargetType
import com.sleekydz86.idolglow.history.domain.UserContentView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserContentViewJpaRepository : JpaRepository<UserContentView, Long> {
    fun findByUserIdAndContentTypeAndContentKey(
        userId: Long,
        contentType: ContentTargetType,
        contentKey: String,
    ): UserContentView?

    fun findByUserId(
        userId: Long,
        pageable: Pageable,
    ): Page<UserContentView>

    @Query("select v.id from UserContentView v where v.userId = :userId")
    fun findIdsByUserId(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): List<Long>

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserContentView v where v.userId = :userId")
    fun deleteAllByUserIdInBulk(
        @Param("userId") userId: Long,
    ): Int

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserContentView v where v.id in :ids")
    fun deleteAllByIdInBulk(
        @Param("ids") ids: Collection<Long>,
    ): Int
}
