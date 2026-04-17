package com.sleekydz86.idolglow.subscription.application.port.out

import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory

interface SubscriptionDispatchHistoryPort {
    fun findAllByLatest(): List<SubscriptionDispatchHistory>
    fun save(history: SubscriptionDispatchHistory): SubscriptionDispatchHistory
    fun count(): Long
    fun existsByContentTypeAndContentSlug(
        contentType: SubscriptionContentType,
        contentSlug: String,
    ): Boolean
}
