package com.sleekydz86.idolglow.subscription.application.dto

import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.MailSendLog
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailConsentSource
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailPreferredBasis
import com.sleekydz86.idolglow.subscription.domain.SubscriptionAudience
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipient
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipientStatus
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSkipReason

data class AdminEmailSubscriberResult(
    val id: Long,
    val email: String,
    val newsletter: Boolean,
    val idolglowIssue: Boolean,
    val subscribedTargets: List<String>,
    val subscribedTargetLabels: List<String>,
    val active: Boolean,
    val statusLabel: String,
    val source: String,
    val consentedAt: String,
    val subscribedAt: String,
) {
    companion object {
        fun from(subscription: EmailSubscription): AdminEmailSubscriberResult {
            val targets = subscription.subscribedTargets()
            return AdminEmailSubscriberResult(
                id = subscription.id,
                email = ProductRecommendationMailEmailMasker.mask(subscription.email),
                newsletter = SubscriptionAudience.NEWSLETTER in targets,
                idolglowIssue = SubscriptionAudience.IDOLGLOW_ISSUE in targets,
                subscribedTargets = targets.map { it.name },
                subscribedTargetLabels = targets.map { it.label },
                active = subscription.active,
                statusLabel = activeLabel(subscription.active),
                source = subscription.subscriptionSource,
                consentedAt = subscription.consentedAt.asSubscriptionDateTime(),
                subscribedAt = subscription.subscribedAt.asSubscriptionDateTime(),
            )
        }
    }
}

data class AdminRecommendationSubscriberResult(
    val id: Long,
    val userId: Long,
    val email: String,
    val nickname: String,
    val active: Boolean,
    val statusLabel: String,
    val preferredBasis: String,
    val preferredBasisLabel: String,
    val consentSource: String,
    val consentSourceLabel: String,
    val consentedAt: String,
    val unsubscribedAt: String?,
    val lastSentAt: String?,
    val consentRemindedAt: String?,
) {
    companion object {
        fun from(row: ProductRecommendationMailSubscriberRow): AdminRecommendationSubscriberResult {
            val subscription = row.subscription
            return AdminRecommendationSubscriberResult(
                id = subscription.id,
                userId = subscription.userId,
                email = ProductRecommendationMailEmailMasker.mask(row.email),
                nickname = row.nickname,
                active = subscription.active,
                statusLabel = activeLabel(subscription.active),
                preferredBasis = subscription.preferredBasis.name,
                preferredBasisLabel = subscription.preferredBasis.label(),
                consentSource = subscription.consentSource.name,
                consentSourceLabel = subscription.consentSource.label(),
                consentedAt = subscription.consentedAt.asSubscriptionDateTime(),
                unsubscribedAt = subscription.unsubscribedAt?.asSubscriptionDateTime(),
                lastSentAt = subscription.lastSentAt?.asSubscriptionDateTime(),
                consentRemindedAt = subscription.consentRemindedAt?.asSubscriptionDateTime(),
            )
        }
    }
}

data class AdminDispatchRecipientResult(
    val id: Long,
    val email: String,
    val status: String,
    val statusLabel: String,
    val sentAt: String?,
    val error: String?,
    val skipReason: String?,
    val skipReasonLabel: String?,
) {
    companion object {
        fun from(recipient: SubscriptionDispatchRecipient): AdminDispatchRecipientResult =
            AdminDispatchRecipientResult(
                id = recipient.id,
                email = ProductRecommendationMailEmailMasker.mask(recipient.email),
                status = recipient.status.name,
                statusLabel = recipient.status.label(),
                sentAt = recipient.sentAt?.asSubscriptionDateTime(),
                error = recipient.errorMessage,
                skipReason = recipient.skipReason?.name,
                skipReasonLabel = recipient.skipReason?.label(),
            )
    }
}

data class AdminDispatchMailContentResult(
    val mailLogId: Long,
    val subject: String,
    val htmlBody: String?,
    val textBody: String?,
    val status: String,
    val statusLabel: String,
    val sentAt: String?,
    val createdAt: String,
) {
    companion object {
        fun from(log: MailSendLog): AdminDispatchMailContentResult =
            AdminDispatchMailContentResult(
                mailLogId = log.id,
                subject = log.subject,
                htmlBody = log.htmlBody,
                textBody = log.textBody,
                status = log.status.name,
                statusLabel = log.status.label,
                sentAt = log.sentAt?.asSubscriptionDateTime(),
                createdAt = log.createdAt.asSubscriptionDateTime(),
            )
    }
}

data class AdminSubscriptionDispatchDetailResult(
    val dispatch: SubscriptionDispatchResult,
    val lastAttemptAt: String?,
    val recipients: OffsetPageResult<AdminDispatchRecipientResult>,
    val mailContent: AdminDispatchMailContentResult?,
)

data class AdminMailSendLogResult(
    val id: Long,
    val kind: String,
    val kindLabel: String,
    val recipientEmail: String,
    val subject: String,
    val status: String,
    val statusLabel: String,
    val error: String?,
    val dispatchId: Long?,
    val sentAt: String?,
    val createdAt: String,
) {
    companion object {
        fun from(log: MailSendLog): AdminMailSendLogResult =
            AdminMailSendLogResult(
                id = log.id,
                kind = log.mailKind.name,
                kindLabel = log.mailKind.label,
                recipientEmail = ProductRecommendationMailEmailMasker.mask(log.recipientEmail),
                subject = log.subject,
                status = log.status.name,
                statusLabel = log.status.label,
                error = log.error,
                dispatchId = log.dispatchId,
                sentAt = log.sentAt?.asSubscriptionDateTime(),
                createdAt = log.createdAt.asSubscriptionDateTime(),
            )
    }
}

data class AdminMailSendLogDetailResult(
    val id: Long,
    val kind: String,
    val kindLabel: String,
    val recipientEmail: String,
    val subject: String,
    val status: String,
    val statusLabel: String,
    val error: String?,
    val dispatchId: Long?,
    val sentAt: String?,
    val createdAt: String,
    val htmlBody: String?,
    val textBody: String?,
) {
    companion object {
        fun from(log: MailSendLog): AdminMailSendLogDetailResult {
            val summary = AdminMailSendLogResult.from(log)
            return AdminMailSendLogDetailResult(
                id = summary.id,
                kind = summary.kind,
                kindLabel = summary.kindLabel,
                recipientEmail = summary.recipientEmail,
                subject = summary.subject,
                status = summary.status,
                statusLabel = summary.statusLabel,
                error = summary.error,
                dispatchId = summary.dispatchId,
                sentAt = summary.sentAt,
                createdAt = summary.createdAt,
                htmlBody = log.htmlBody,
                textBody = log.textBody,
            )
        }
    }
}

private fun activeLabel(active: Boolean): String = if (active) "구독 중" else "해지"

private fun ProductRecommendationMailPreferredBasis.label(): String =
    when (this) {
        ProductRecommendationMailPreferredBasis.AUTO -> "취향 우선"
        ProductRecommendationMailPreferredBasis.POPULAR -> "인기순"
    }

private fun ProductRecommendationMailConsentSource.label(): String =
    when (this) {
        ProductRecommendationMailConsentSource.MY_SURVEY -> "회원 설문"
        ProductRecommendationMailConsentSource.MYPAGE -> "마이페이지"
    }

private fun SubscriptionDispatchRecipientStatus.label(): String =
    when (this) {
        SubscriptionDispatchRecipientStatus.SENT -> "발송"
        SubscriptionDispatchRecipientStatus.FAILED -> "실패"
        SubscriptionDispatchRecipientStatus.SKIPPED -> "건너뜀"
    }

private fun SubscriptionDispatchSkipReason.label(): String =
    when (this) {
        SubscriptionDispatchSkipReason.NO_PRODUCTS -> "보낼 상품 없음"
    }
