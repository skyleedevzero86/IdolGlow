package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ProductRecommendationMailSubscriptionJpaRepository :
    JpaRepository<ProductRecommendationMailSubscription, Long> {
    fun findByUserId(userId: Long): ProductRecommendationMailSubscription?

    fun findAllByActiveTrueOrderByIdAsc(): List<ProductRecommendationMailSubscription>

    @Query(
        """
        select s from ProductRecommendationMailSubscription s
        where s.active = true
          and coalesce(s.consentRemindedAt, s.consentedAt) <= :lastNoticeAtOrBefore
        order by s.id asc
        """,
    )
    fun findAllConsentReminderDue(
        @Param("lastNoticeAtOrBefore") lastNoticeAtOrBefore: LocalDateTime,
    ): List<ProductRecommendationMailSubscription>
}
