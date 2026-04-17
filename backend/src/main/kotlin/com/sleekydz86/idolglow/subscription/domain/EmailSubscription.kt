package com.sleekydz86.idolglow.subscription.domain

import com.sleekydz86.idolglow.global.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "email_subscriptions",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_email_subscriptions_email", columnNames = ["email"]),
    ]
)
class EmailSubscription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false, length = 255)
    var email: String,

    @Column(name = "subscribed_newsletters", nullable = false)
    var subscribedNewsletters: Boolean,

    @Column(name = "subscribed_issues", nullable = false)
    var subscribedIssues: Boolean,

    @Column(name = "consented_at", nullable = false)
    var consentedAt: LocalDateTime,

    @Column(name = "subscribed_at", nullable = false)
    var subscribedAt: LocalDateTime,

    @Column(name = "subscription_source", nullable = false, length = 50)
    var subscriptionSource: String,

    @Column(nullable = false)
    var active: Boolean,
) : BaseEntity() {

    fun resubscribe(
        subscribedNewsletters: Boolean,
        subscribedIssues: Boolean,
        consentedAt: LocalDateTime,
        subscribedAt: LocalDateTime,
        subscriptionSource: String,
    ) {
        require(subscribedNewsletters || subscribedIssues) {
            "소식지 또는 웹진 호 중 하나 이상을 선택해야 합니다."
        }

        this.subscribedNewsletters = subscribedNewsletters
        this.subscribedIssues = subscribedIssues
        this.consentedAt = consentedAt
        this.subscribedAt = subscribedAt
        this.subscriptionSource = subscriptionSource.trim()
        this.active = true
    }

    fun subscribedTargets(): List<SubscriptionAudience> = buildList {
        if (subscribedNewsletters) add(SubscriptionAudience.NEWSLETTER)
        if (subscribedIssues) add(SubscriptionAudience.WEBZINE_ISSUE)
    }

    companion object {
        fun create(
            email: String,
            subscribedNewsletters: Boolean,
            subscribedIssues: Boolean,
            consentedAt: LocalDateTime,
            subscribedAt: LocalDateTime,
            subscriptionSource: String,
        ): EmailSubscription {
            require(email.isNotBlank()) { "구독 이메일은 비울 수 없습니다." }
            require(subscribedNewsletters || subscribedIssues) {
                "소식지 또는 웹진 호 중 하나 이상을 선택해야 합니다."
            }

            return EmailSubscription(
                email = email.trim().lowercase(),
                subscribedNewsletters = subscribedNewsletters,
                subscribedIssues = subscribedIssues,
                consentedAt = consentedAt,
                subscribedAt = subscribedAt,
                subscriptionSource = subscriptionSource.trim(),
                active = true,
            )
        }
    }
}

enum class SubscriptionAudience(val label: String) {
    NEWSLETTER("소식지"),
    WEBZINE_ISSUE("호별보기"),
}
