package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchHistoryPort
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchChannel
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class SubscriptionDispatchHistoryRepositoryImpl(
    private val subscriptionDispatchHistoryJpaRepository: SubscriptionDispatchHistoryJpaRepository,
) : SubscriptionDispatchHistoryPort {
    override fun findAllByLatest(): List<SubscriptionDispatchHistory> =
        subscriptionDispatchHistoryJpaRepository.findAll(
            Sort.by(
                Sort.Order.desc("dispatchedAt"),
                Sort.Order.desc("createdAt"),
            ),
        )

    override fun save(history: SubscriptionDispatchHistory): SubscriptionDispatchHistory =
        subscriptionDispatchHistoryJpaRepository.save(history)

    override fun saveAndFlush(history: SubscriptionDispatchHistory): SubscriptionDispatchHistory =
        subscriptionDispatchHistoryJpaRepository.saveAndFlush(history)

    override fun count(): Long = subscriptionDispatchHistoryJpaRepository.count()

    override fun existsByContentTypeAndContentSlug(
        contentType: SubscriptionContentType,
        contentSlug: String,
    ): Boolean =
        subscriptionDispatchHistoryJpaRepository.existsByContentTypeAndContentSlugAndDispatchChannel(
            contentType = contentType,
            contentSlug = contentSlug,
            dispatchChannel = SubscriptionDispatchChannel.EMAIL,
        )

    override fun findByContent(
        contentType: SubscriptionContentType,
        contentSlug: String,
    ): SubscriptionDispatchHistory? =
        subscriptionDispatchHistoryJpaRepository.findByContentTypeAndContentSlugAndDispatchChannel(
            contentType = contentType,
            contentSlug = contentSlug,
            dispatchChannel = SubscriptionDispatchChannel.EMAIL,
        )

    override fun findById(id: Long): SubscriptionDispatchHistory? =
        subscriptionDispatchHistoryJpaRepository.findById(id).orElse(null)

    override fun claimResume(
        id: Long,
        now: LocalDateTime,
        staleBefore: LocalDateTime,
    ): Boolean =
        subscriptionDispatchHistoryJpaRepository.claimResume(
            id = id,
            now = now,
            staleBefore = staleBefore,
        ) > 0

    override fun findStaleSending(staleBefore: LocalDateTime): List<SubscriptionDispatchHistory> =
        subscriptionDispatchHistoryJpaRepository.findStaleSending(staleBefore)
}
