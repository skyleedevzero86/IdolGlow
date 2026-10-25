package com.sleekydz86.idolglow.subscription.application.port.out

import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import java.time.LocalDateTime

interface SubscriptionDispatchHistoryPort {
    fun findAllByLatest(): List<SubscriptionDispatchHistory>

    fun save(history: SubscriptionDispatchHistory): SubscriptionDispatchHistory

    fun saveAndFlush(history: SubscriptionDispatchHistory): SubscriptionDispatchHistory

    fun count(): Long

    fun existsByContentTypeAndContentSlug(
        contentType: SubscriptionContentType,
        contentSlug: String,
    ): Boolean

    fun findByContent(
        contentType: SubscriptionContentType,
        contentSlug: String,
    ): SubscriptionDispatchHistory?

    fun findById(id: Long): SubscriptionDispatchHistory?

    fun claimResume(
        id: Long,
        now: LocalDateTime,
        staleBefore: LocalDateTime,
    ): Boolean

    fun findStaleSending(staleBefore: LocalDateTime): List<SubscriptionDispatchHistory>
}
