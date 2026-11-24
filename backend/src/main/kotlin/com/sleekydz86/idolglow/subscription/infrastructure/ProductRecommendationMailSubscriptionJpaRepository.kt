package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailSubscriberRow
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailPreferredBasis
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

    @Query(
        value = """
        select new com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailSubscriberRow(
            s, u.email, u.nickname.value
        )
        from ProductRecommendationMailSubscription s
        join User u on u.id = s.userId
        where (lower(u.email) like :pattern escape '!' or lower(u.nickname.value) like :pattern escape '!')
          and s.active in :actives
          and s.preferredBasis in :bases
        order by s.consentedAt desc, s.id desc
        """,
        countQuery = """
        select count(s)
        from ProductRecommendationMailSubscription s
        join User u on u.id = s.userId
        where (lower(u.email) like :pattern escape '!' or lower(u.nickname.value) like :pattern escape '!')
          and s.active in :actives
          and s.preferredBasis in :bases
        """,
    )
    fun searchSubscribers(
        @Param("pattern") pattern: String,
        @Param("actives") actives: Collection<Boolean>,
        @Param("bases") bases: Collection<ProductRecommendationMailPreferredBasis>,
        pageable: Pageable,
    ): Page<ProductRecommendationMailSubscriberRow>
}
