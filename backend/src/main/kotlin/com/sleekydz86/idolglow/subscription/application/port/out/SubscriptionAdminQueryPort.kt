package com.sleekydz86.idolglow.subscription.application.port.out

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetSlice
import com.sleekydz86.idolglow.subscription.application.dto.DispatchHistorySearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.DispatchRecipientSearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.EmailSubscriberSearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailSubscriberRow
import com.sleekydz86.idolglow.subscription.application.dto.RecommendationSubscriberSearchCondition
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipient

interface SubscriptionAdminQueryPort {
    fun searchEmailSubscribers(
        condition: EmailSubscriberSearchCondition,
        request: OffsetPageRequest,
    ): OffsetSlice<EmailSubscription>

    fun searchRecommendationSubscribers(
        condition: RecommendationSubscriberSearchCondition,
        request: OffsetPageRequest,
    ): OffsetSlice<ProductRecommendationMailSubscriberRow>

    fun searchDispatches(
        condition: DispatchHistorySearchCondition,
        request: OffsetPageRequest,
    ): OffsetSlice<SubscriptionDispatchHistory>

    fun searchDispatchRecipients(
        condition: DispatchRecipientSearchCondition,
        request: OffsetPageRequest,
    ): OffsetSlice<SubscriptionDispatchRecipient>
}
