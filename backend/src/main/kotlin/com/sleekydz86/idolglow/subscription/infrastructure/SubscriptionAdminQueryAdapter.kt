package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetSlice
import com.sleekydz86.idolglow.subscription.application.dto.DispatchHistorySearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.DispatchRecipientSearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.EmailSubscriberSearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailSubscriberRow
import com.sleekydz86.idolglow.subscription.application.dto.RecommendationSubscriberSearchCondition
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionAdminQueryPort
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailPreferredBasis
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipient
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class SubscriptionAdminQueryAdapter(
    private val emailSubscriptionJpaRepository: EmailSubscriptionJpaRepository,
    private val productRecommendationMailSubscriptionJpaRepository: ProductRecommendationMailSubscriptionJpaRepository,
    private val subscriptionDispatchHistoryJpaRepository: SubscriptionDispatchHistoryJpaRepository,
    private val subscriptionDispatchRecipientJpaRepository: SubscriptionDispatchRecipientJpaRepository,
) : SubscriptionAdminQueryPort {
    override fun searchEmailSubscribers(
        condition: EmailSubscriberSearchCondition,
        request: OffsetPageRequest,
    ): OffsetSlice<EmailSubscription> =
        emailSubscriptionJpaRepository
            .findAll(
                SubscriptionAdminSpecifications.emailSubscribers(condition),
                PageRequest.of(request.page, request.size, EMAIL_SUBSCRIBER_SORT),
            ).toSlice()

    override fun searchRecommendationSubscribers(
        condition: RecommendationSubscriberSearchCondition,
        request: OffsetPageRequest,
    ): OffsetSlice<ProductRecommendationMailSubscriberRow> =
        productRecommendationMailSubscriptionJpaRepository
            .searchSubscribers(
                pattern = SubscriptionAdminSpecifications.containsPattern(condition.keyword),
                actives = condition.status?.let { listOf(it.active) } ?: listOf(true, false),
                bases = condition.basis?.let { listOf(it) } ?: ProductRecommendationMailPreferredBasis.entries,
                pageable = PageRequest.of(request.page, request.size),
            ).toSlice()

    override fun searchDispatches(
        condition: DispatchHistorySearchCondition,
        request: OffsetPageRequest,
    ): OffsetSlice<SubscriptionDispatchHistory> =
        subscriptionDispatchHistoryJpaRepository
            .findAll(
                SubscriptionAdminSpecifications.dispatches(condition),
                PageRequest.of(request.page, request.size, DISPATCH_SORT),
            ).toSlice()

    override fun searchDispatchRecipients(
        condition: DispatchRecipientSearchCondition,
        request: OffsetPageRequest,
    ): OffsetSlice<SubscriptionDispatchRecipient> =
        subscriptionDispatchRecipientJpaRepository
            .findAll(
                SubscriptionAdminSpecifications.dispatchRecipients(condition),
                PageRequest.of(request.page, request.size, RECIPIENT_SORT),
            ).toSlice()

    private fun <T : Any> Page<T>.toSlice(): OffsetSlice<T> = OffsetSlice(content, totalElements)

    companion object {
        private val EMAIL_SUBSCRIBER_SORT: Sort = Sort.by(Sort.Order.desc("subscribedAt"), Sort.Order.desc("id"))
        private val DISPATCH_SORT: Sort = Sort.by(Sort.Order.desc("dispatchedAt"), Sort.Order.desc("id"))
        private val RECIPIENT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
