package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchChannel
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistoryRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class SubscriptionDispatchHistoryRepositoryImpl(
    private val subscriptionDispatchHistoryJpaRepository: SubscriptionDispatchHistoryJpaRepository,
) : SubscriptionDispatchHistoryRepository {

    override fun findAllByLatest(): List<SubscriptionDispatchHistory> =
        subscriptionDispatchHistoryJpaRepository.findAll(
            Sort.by(
                Sort.Order.desc("dispatchedAt"),
                Sort.Order.desc("createdAt"),
            )
        )

    override fun save(history: SubscriptionDispatchHistory): SubscriptionDispatchHistory =
        subscriptionDispatchHistoryJpaRepository.save(history)

    override fun count(): Long =
        subscriptionDispatchHistoryJpaRepository.count()

    override fun existsByContentTypeAndContentSlug(
        contentType: SubscriptionContentType,
        contentSlug: String,
    ): Boolean =
        subscriptionDispatchHistoryJpaRepository.existsByContentTypeAndContentSlugAndDispatchChannel(
            contentType = contentType,
            contentSlug = contentSlug,
            dispatchChannel = SubscriptionDispatchChannel.EMAIL,
        )
}
