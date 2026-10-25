package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.subscription.application.event.NewsletterDispatchRequestedEvent
import com.sleekydz86.idolglow.subscription.application.event.WebzineIssueDispatchRequestedEvent
import com.sleekydz86.idolglow.subscription.application.port.out.EmailSubscriptionPort
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import com.sleekydz86.idolglow.subscription.domain.SubscriptionAudience
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchStatus
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SubscriptionDispatchMailService(
    private val appMailProperties: AppMailProperties,
    private val emailSubscriptionPort: EmailSubscriptionPort,
    private val outboundMailPort: OutboundMailPort,
    private val subscriptionEmailComposer: SubscriptionEmailComposer,
    private val subscriptionDispatchTxService: SubscriptionDispatchTxService,
) {
    private val log = LoggerFactory.getLogger(SubscriptionDispatchMailService::class.java)

    fun dispatchNewsletter(event: NewsletterDispatchRequestedEvent) {
        dispatchIfNeeded(
            draft =
                SubscriptionDispatchHistory.record(
                    contentType = SubscriptionContentType.NEWSLETTER,
                    contentSlug = event.slug,
                    contentTitle = event.title,
                    contentSummary = event.summary,
                    recipientCount = 0,
                    contentCreatedAt = event.contentCreatedAt,
                    dispatchedAt = LocalDateTime.now(),
                    dispatchStatus = SubscriptionDispatchStatus.SENDING,
                ),
            audience = SubscriptionAudience.NEWSLETTER,
        ) {
            subscriptionEmailComposer.composeNewsletter(event)
        }
    }

    fun dispatchWebzineIssue(event: WebzineIssueDispatchRequestedEvent) {
        dispatchIfNeeded(
            draft =
                SubscriptionDispatchHistory.record(
                    contentType = SubscriptionContentType.WEBZINE_ISSUE,
                    contentSlug = event.slug,
                    contentTitle = "Vol.${event.volume} 웹진",
                    contentSummary = event.teaser,
                    recipientCount = 0,
                    contentCreatedAt = event.contentCreatedAt,
                    dispatchedAt = LocalDateTime.now(),
                    dispatchStatus = SubscriptionDispatchStatus.SENDING,
                ),
            audience = SubscriptionAudience.WEBZINE_ISSUE,
        ) {
            subscriptionEmailComposer.composeWebzineIssue(event)
        }
    }

    private fun dispatchIfNeeded(
        draft: SubscriptionDispatchHistory,
        audience: SubscriptionAudience,
        emailFactory: () -> ComposedSubscriptionEmail,
    ) {
        val history = claimOrResume(draft, audience) ?: return
        deliver(history, audience, emailFactory)
    }

    private fun claimOrResume(
        draft: SubscriptionDispatchHistory,
        audience: SubscriptionAudience,
    ): SubscriptionDispatchHistory? {
        val now = LocalDateTime.now()
        val staleBefore = now.minusMinutes(SubscriptionDispatchHistory.RESUME_AFTER_MINUTES)
        val existing = subscriptionDispatchTxService.findClaimed(draft.contentType, draft.contentSlug)
        if (existing != null) {
            return resumeIfStale(existing, staleBefore, now)
        }
        return insertFresh(draft, audience)
    }

    private fun insertFresh(
        draft: SubscriptionDispatchHistory,
        audience: SubscriptionAudience,
    ): SubscriptionDispatchHistory? =
        try {
            val enabled = appMailProperties.enabled
            val saved =
                subscriptionDispatchTxService.insertClaim(
                    SubscriptionDispatchHistory.record(
                        contentType = draft.contentType,
                        contentSlug = draft.contentSlug,
                        contentTitle = draft.contentTitle,
                        contentSummary = draft.contentSummary,
                        recipientCount = if (enabled) 0 else audienceSize(audience),
                        contentCreatedAt = draft.contentCreatedAt,
                        dispatchedAt = draft.dispatchedAt,
                        dispatchStatus = claimStatus(enabled),
                    ),
                )
            saved.takeIf { it.dispatchStatus == SubscriptionDispatchStatus.SENDING }
        } catch (ex: DataIntegrityViolationException) {
            log.info(
                "구독 발송 이력이 이미 있어 선점에 실패했습니다. type={} slug={} cause={}",
                draft.contentType,
                draft.contentSlug,
                ex.javaClass.simpleName,
            )
            null
        }

    private fun resumeIfStale(
        existing: SubscriptionDispatchHistory,
        staleBefore: LocalDateTime,
        now: LocalDateTime,
    ): SubscriptionDispatchHistory? {
        val won =
            appMailProperties.enabled &&
                existing.isStaleSending(staleBefore) &&
                subscriptionDispatchTxService.claimResume(existing.id, now, staleBefore)
        return if (won) existing else null
    }

    private fun deliver(
        history: SubscriptionDispatchHistory,
        audience: SubscriptionAudience,
        emailFactory: () -> ComposedSubscriptionEmail,
    ) {
        val composed = emailFactory()
        val alreadySent = subscriptionDispatchTxService.sentEmails(history.id)
        emailSubscriptionPort
            .findActiveEmailsByAudience(audience)
            .filterNot { it in alreadySent }
            .forEach { recipient -> sendOne(history.id, composed, recipient) }
        subscriptionDispatchTxService.finish(history.id)
    }

    private fun sendOne(
        dispatchId: Long,
        email: ComposedSubscriptionEmail,
        recipient: String,
    ) {
        try {
            outboundMailPort.send(
                OutboundMailMessage(
                    to = recipient,
                    subject = email.subject,
                    plainTextBody = email.plainText,
                    htmlBody = email.htmlBody,
                ),
            )
            subscriptionDispatchTxService.recordOutcome(
                dispatchId = dispatchId,
                email = recipient,
                sent = true,
                error = null,
                sentAt = LocalDateTime.now(),
            )
        } catch (ex: Exception) {
            log.error("구독 메일 발송 실패: 수신={}, 제목={}", recipient, email.subject, ex)
            subscriptionDispatchTxService.recordOutcome(
                dispatchId = dispatchId,
                email = recipient,
                sent = false,
                error = ex.message,
                sentAt = LocalDateTime.now(),
            )
        }
    }

    private fun audienceSize(audience: SubscriptionAudience): Long =
        emailSubscriptionPort.findActiveEmailsByAudience(audience).size.toLong()

    private fun claimStatus(enabled: Boolean): SubscriptionDispatchStatus =
        if (enabled) SubscriptionDispatchStatus.SENDING else SubscriptionDispatchStatus.RECORDED
}
