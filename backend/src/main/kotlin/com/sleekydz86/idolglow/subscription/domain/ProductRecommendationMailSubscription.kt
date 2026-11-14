package com.sleekydz86.idolglow.subscription.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "product_recommendation_mail_subscriptions",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_product_reco_mail_subscriptions_user", columnNames = ["user_id"]),
    ],
)
class ProductRecommendationMailSubscription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(nullable = false)
    var active: Boolean,
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_basis", nullable = false, length = 20)
    var preferredBasis: ProductRecommendationMailPreferredBasis,
    @Column(name = "consented_at", nullable = false)
    var consentedAt: LocalDateTime,
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_source", nullable = false, length = 20)
    var consentSource: ProductRecommendationMailConsentSource,
    @Column(name = "token_version", nullable = false)
    var tokenVersion: Int = 1,
    @Column(name = "unsubscribed_at")
    var unsubscribedAt: LocalDateTime? = null,
    @Column(name = "last_sent_at")
    var lastSentAt: LocalDateTime? = null,
    @Column(name = "last_sent_product_ids", length = 200)
    var lastSentProductIds: String? = null,
    @Column(name = "consent_reminded_at")
    var consentRemindedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun subscribe(
        consentSource: ProductRecommendationMailConsentSource,
        now: LocalDateTime,
    ) {
        if (active) return
        active = true
        tokenVersion += 1
        consentedAt = now
        this.consentSource = consentSource
        unsubscribedAt = null
        consentRemindedAt = null
    }

    fun unsubscribe(now: LocalDateTime) {
        if (!active) return
        active = false
        unsubscribedAt = now
    }

    fun changeBasis(preferredBasis: ProductRecommendationMailPreferredBasis) {
        this.preferredBasis = preferredBasis
    }

    fun recordSent(
        productIds: List<Long>,
        now: LocalDateTime,
    ) {
        lastSentAt = now
        lastSentProductIds =
            productIds
                .distinct()
                .fold("") { joined, id ->
                    val next = if (joined.isEmpty()) id.toString() else "$joined$PRODUCT_ID_SEPARATOR$id"
                    if (next.length <= LAST_SENT_PRODUCT_IDS_MAX_LENGTH) next else joined
                }.ifEmpty { null }
    }

    fun lastSentProductIdSet(): Set<Long> =
        lastSentProductIds
            ?.split(PRODUCT_ID_SEPARATOR)
            ?.mapNotNull { it.trim().toLongOrNull() }
            ?.toSet()
            .orEmpty()

    companion object {
        private const val PRODUCT_ID_SEPARATOR = ","
        private const val LAST_SENT_PRODUCT_IDS_MAX_LENGTH = 200

        fun create(
            userId: Long,
            consentSource: ProductRecommendationMailConsentSource,
            preferredBasis: ProductRecommendationMailPreferredBasis,
            now: LocalDateTime,
        ): ProductRecommendationMailSubscription {
            require(userId > 0) { "회원 ID는 0보다 커야 합니다. userId=$userId" }
            return ProductRecommendationMailSubscription(
                userId = userId,
                active = true,
                preferredBasis = preferredBasis,
                consentedAt = now,
                consentSource = consentSource,
            )
        }
    }
}
