package com.sleekydz86.idolglow.subscription.application.dto

import com.sleekydz86.idolglow.subscription.domain.MailSendKind
import com.sleekydz86.idolglow.subscription.domain.MailSendStatus
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailPreferredBasis
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import com.sleekydz86.idolglow.subscription.domain.SubscriptionAudience
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipientStatus
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchStatus
import java.time.LocalDate
import java.time.LocalDateTime

enum class SubscriberStatusFilter(
    val active: Boolean,
) {
    ACTIVE(true),
    INACTIVE(false),
}

data class AdminDateRange(
    val fromInclusive: LocalDateTime?,
    val toExclusive: LocalDateTime?,
) {
    companion object {
        val ALL = AdminDateRange(null, null)

        fun of(
            from: LocalDate?,
            to: LocalDate?,
        ): AdminDateRange {
            require(from == null || to == null || !from.isAfter(to)) {
                "조회 시작일은 종료일보다 늦을 수 없습니다. from=$from to=$to"
            }
            return AdminDateRange(from?.atStartOfDay(), to?.plusDays(1)?.atStartOfDay())
        }
    }
}

data class EmailSubscriberSearchCondition(
    val keyword: String? = null,
    val audience: SubscriptionAudience? = null,
    val status: SubscriberStatusFilter? = null,
)

data class RecommendationSubscriberSearchCondition(
    val keyword: String? = null,
    val status: SubscriberStatusFilter? = null,
    val basis: ProductRecommendationMailPreferredBasis? = null,
)

data class DispatchHistorySearchCondition(
    val keyword: String? = null,
    val contentType: SubscriptionContentType? = null,
    val status: SubscriptionDispatchStatus? = null,
    val range: AdminDateRange = AdminDateRange.ALL,
)

data class DispatchRecipientSearchCondition(
    val dispatchId: Long,
    val keyword: String? = null,
    val status: SubscriptionDispatchRecipientStatus? = null,
)

data class MailSendLogSearchCondition(
    val keyword: String? = null,
    val kind: MailSendKind? = null,
    val status: MailSendStatus? = null,
    val range: AdminDateRange = AdminDateRange.ALL,
)

data class ProductRecommendationMailSubscriberRow(
    val subscription: ProductRecommendationMailSubscription,
    val email: String,
    val nickname: String,
)

internal fun String?.normalizedKeyword(): String? = this?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
