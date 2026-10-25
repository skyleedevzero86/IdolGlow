package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchChannel
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface SubscriptionDispatchHistoryJpaRepository : JpaRepository<SubscriptionDispatchHistory, Long> {
    fun existsByContentTypeAndContentSlugAndDispatchChannel(
        contentType: SubscriptionContentType,
        contentSlug: String,
        dispatchChannel: SubscriptionDispatchChannel,
    ): Boolean

    fun findByContentTypeAndContentSlugAndDispatchChannel(
        contentType: SubscriptionContentType,
        contentSlug: String,
        dispatchChannel: SubscriptionDispatchChannel,
    ): SubscriptionDispatchHistory?

    @Query(
        """
        select h from SubscriptionDispatchHistory h
        where h.dispatchStatus = com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchStatus.SENDING
          and (h.lastAttemptAt is null or h.lastAttemptAt <= :staleBefore)
        """,
    )
    fun findStaleSending(staleBefore: LocalDateTime): List<SubscriptionDispatchHistory>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update SubscriptionDispatchHistory h
           set h.lastAttemptAt = :now
         where h.id = :id
           and h.dispatchStatus = com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchStatus.SENDING
           and (h.lastAttemptAt is null or h.lastAttemptAt <= :staleBefore)
        """,
    )
    fun claimResume(
        id: Long,
        now: LocalDateTime,
        staleBefore: LocalDateTime,
    ): Int
}
