package com.sleekydz86.idolglow.subscription.domain

interface SubscriptionDispatchHistoryRepository {
    fun findAllByLatest(): List<SubscriptionDispatchHistory>
    fun save(history: SubscriptionDispatchHistory): SubscriptionDispatchHistory
    fun count(): Long
    fun existsByContentTypeAndContentSlug(
        contentType: SubscriptionContentType,
        contentSlug: String,
    ): Boolean
}
