package com.sleekydz86.idolglow.like.infrastructure

import com.sleekydz86.idolglow.like.domain.ContentLike
import com.sleekydz86.idolglow.like.domain.LikeTargetType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ContentLikeJpaRepository : JpaRepository<ContentLike, Long> {
    fun existsByUserIdAndTargetTypeAndTargetKey(
        userId: Long,
        targetType: LikeTargetType,
        targetKey: String,
    ): Boolean

    fun countByTargetTypeAndTargetKey(
        targetType: LikeTargetType,
        targetKey: String,
    ): Long

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        "delete from ContentLike l " +
            "where l.userId = :userId and l.targetType = :targetType and l.targetKey = :targetKey",
    )
    fun deleteByUserAndTarget(
        @Param("userId") userId: Long,
        @Param("targetType") targetType: LikeTargetType,
        @Param("targetKey") targetKey: String,
    ): Int

    @Query(
        "select l.targetKey, count(l) from ContentLike l " +
            "where l.targetType = :targetType and l.targetKey in :targetKeys group by l.targetKey",
    )
    fun countGroupedByKey(
        @Param("targetType") targetType: LikeTargetType,
        @Param("targetKeys") targetKeys: Collection<String>,
    ): List<Array<Any>>

    @Query(
        "select l.targetKey from ContentLike l " +
            "where l.userId = :userId and l.targetType = :targetType and l.targetKey in :targetKeys",
    )
    fun findLikedKeys(
        @Param("userId") userId: Long,
        @Param("targetType") targetType: LikeTargetType,
        @Param("targetKeys") targetKeys: Collection<String>,
    ): List<String>
}
