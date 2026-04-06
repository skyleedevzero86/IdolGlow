package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchChannel
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import org.springframework.data.jpa.repository.JpaRepository

interface SubscriptionDispatchHistoryJpaRepository : JpaRepository<SubscriptionDispatchHistory, Long> {
    fun existsByContentTypeAndContentSlugAndDispatchChannel(
        contentType: SubscriptionContentType,
        contentSlug: String,
        dispatchChannel: SubscriptionDispatchChannel,
    ): Boolean
}
