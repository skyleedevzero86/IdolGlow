package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.subscription.application.event.NewsletterDispatchRequestedEvent
import com.sleekydz86.idolglow.subscription.application.event.WebzineIssueDispatchRequestedEvent
import com.sleekydz86.idolglow.subscription.application.port.out.EmailSubscriptionPort
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchHistoryPort
import com.sleekydz86.idolglow.subscription.domain.SubscriptionAudience
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SubscriptionDispatchMailService(
    private val appMailProperties: AppMailProperties,
    private val emailSubscriptionPort: EmailSubscriptionPort,
    private val subscriptionDispatchHistoryPort: SubscriptionDispatchHistoryPort,
    private val outboundMailPort: OutboundMailPort,
    private val subscriptionEmailComposer: SubscriptionEmailComposer,
) {

    private val log = LoggerFactory.getLogger(SubscriptionDispatchMailService::class.java)

    @Transactional
    fun dispatchNewsletter(event: NewsletterDispatchRequestedEvent) {
        dispatchIfNeeded(
            contentType = SubscriptionContentType.NEWSLETTER,
            contentSlug = event.slug,
            contentTitle = event.title,
            contentSummary = event.summary,
            audience = SubscriptionAudience.NEWSLETTER,
            contentCreatedAt = event.contentCreatedAt,
        ) {
            subscriptionEmailComposer.composeNewsletter(event)
        }
    }

    @Transactional
    fun dispatchWebzineIssue(event: WebzineIssueDispatchRequestedEvent) {
        dispatchIfNeeded(
            contentType = SubscriptionContentType.WEBZINE_ISSUE,
            contentSlug = event.slug,
            contentTitle = "Vol.${event.volume} 웹진",
            contentSummary = event.teaser,
            audience = SubscriptionAudience.WEBZINE_ISSUE,
            contentCreatedAt = event.contentCreatedAt,
        ) {
            subscriptionEmailComposer.composeWebzineIssue(event)
        }
    }

    private fun dispatchIfNeeded(
        contentType: SubscriptionContentType,
        contentSlug: String,
        contentTitle: String,
        contentSummary: String?,
        audience: SubscriptionAudience,
        contentCreatedAt: LocalDateTime?,
        emailFactory: () -> ComposedSubscriptionEmail,
    ) {
        if (subscriptionDispatchHistoryPort.existsByContentTypeAndContentSlug(contentType, contentSlug)) {
            return
        }

        val recipients = emailSubscriptionPort.findActiveEmailsByAudience(audience)
        val now = LocalDateTime.now()
        val dispatchStatus = if (!appMailProperties.enabled) {
            SubscriptionDispatchStatus.RECORDED
        } else {
            sendEmails(emailFactory(), recipients)
        }

        subscriptionDispatchHistoryPort.save(
            SubscriptionDispatchHistory.record(
                contentType = contentType,
                contentSlug = contentSlug,
                contentTitle = contentTitle,
                contentSummary = contentSummary,
                recipientCount = recipients.size.toLong(),
                contentCreatedAt = contentCreatedAt,
                dispatchedAt = now,
                dispatchStatus = dispatchStatus,
            )
        )
    }

    private fun sendEmails(
        email: ComposedSubscriptionEmail,
        recipients: List<String>,
    ): SubscriptionDispatchStatus {
        if (recipients.isEmpty()) {
            return SubscriptionDispatchStatus.SENT
        }

        var failedCount = 0
        recipients.forEach { recipient ->
            try {
                outboundMailPort.send(
                    OutboundMailMessage(
                        to = recipient,
                        subject = email.subject,
                        plainTextBody = email.plainText,
                        htmlBody = email.htmlBody,
                    )
                )
            } catch (ex: Exception) {
                failedCount += 1
                log.error("구독 메일 발송 실패: 수신={}, 제목={}", recipient, email.subject, ex)
            }
        }

        return if (failedCount == 0) SubscriptionDispatchStatus.SENT else SubscriptionDispatchStatus.FAILED
    }
}
