package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailPlan
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import com.sleekydz86.idolglow.subscription.domain.MailSendKind
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchFrequency
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSchedule
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSkipReason
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchStatus
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ProductRecommendationMailDispatcher(
    private val appMailProperties: AppMailProperties,
    private val appPublicUrlProperties: AppPublicUrlProperties,
    private val subscriptionDispatchTxService: SubscriptionDispatchTxService,
    private val productRecommendationMailDispatchTxService: ProductRecommendationMailDispatchTxService,
    private val subscriptionEmailComposer: SubscriptionEmailComposer,
    private val outboundMailPort: OutboundMailPort,
) {
    private val log = LoggerFactory.getLogger(ProductRecommendationMailDispatcher::class.java)
    internal var clock: Clock = Clock.system(IdolGlowDateTimes.ZONE)

    fun dispatch(frequency: SubscriptionDispatchFrequency) {
        dispatchSlug(ProductRecommendationMailSlug.of(frequency, LocalDate.now(clock)))
    }

    fun resume(contentSlug: String) {
        dispatchSlug(contentSlug)
    }

    private fun dispatchSlug(contentSlug: String) {
        if (LocalDateTime.now(clock).hour !in SubscriptionDispatchSchedule.ADVERTISING_DISPATCH_HOURS) {
            log.info("상품 추천 메일은 8시부터 20시 사이에만 보냅니다. slug={}", contentSlug)
            return
        }
        val history = claimOrResume(contentSlug) ?: return
        deliver(history)
    }

    private fun claimOrResume(contentSlug: String): SubscriptionDispatchHistory? {
        val now = LocalDateTime.now()
        val staleBefore = now.minusMinutes(SubscriptionDispatchHistory.RESUME_AFTER_MINUTES)
        val existing = subscriptionDispatchTxService.findClaimed(CONTENT_TYPE, contentSlug)
        if (existing != null) {
            val won =
                appMailProperties.enabled &&
                    existing.isStaleSending(staleBefore) &&
                    subscriptionDispatchTxService.claimResume(existing.id, now, staleBefore)
            return existing.takeIf { won }
        }
        return insertFresh(contentSlug, now)
    }

    private fun insertFresh(
        contentSlug: String,
        now: LocalDateTime,
    ): SubscriptionDispatchHistory? =
        try {
            val enabled = appMailProperties.enabled
            val status = if (enabled) SubscriptionDispatchStatus.SENDING else SubscriptionDispatchStatus.RECORDED
            val recipientCount = if (enabled) 0L else productRecommendationMailDispatchTxService.countRecipients()
            subscriptionDispatchTxService
                .insertClaim(
                    SubscriptionDispatchHistory.record(
                        contentType = CONTENT_TYPE,
                        contentSlug = contentSlug,
                        contentTitle = "$TITLE_PREFIX ${ProductRecommendationMailSlug.periodOf(contentSlug)}",
                        contentSummary = null,
                        recipientCount = recipientCount,
                        contentCreatedAt = null,
                        dispatchedAt = now,
                        dispatchStatus = status,
                    ),
                ).takeIf { it.dispatchStatus == SubscriptionDispatchStatus.SENDING }
        } catch (ex: DataIntegrityViolationException) {
            log.info(
                "상품 추천 메일 발송 이력이 이미 있어 선점에 실패했습니다. slug={} cause={}",
                contentSlug,
                ex.javaClass.simpleName,
            )
            null
        }

    private fun deliver(history: SubscriptionDispatchHistory) {
        val alreadySent = subscriptionDispatchTxService.sentEmails(history.id)
        productRecommendationMailDispatchTxService
            .plan(alreadySent)
            .forEach { plan -> sendOne(history.id, plan) }
        subscriptionDispatchTxService.finish(history.id)
    }

    private fun sendOne(
        dispatchId: Long,
        plan: ProductRecommendationMailPlan,
    ) {
        if (plan.items.isEmpty()) {
            subscriptionDispatchTxService.recordSkipped(
                dispatchId = dispatchId,
                email = plan.email,
                reason = SubscriptionDispatchSkipReason.NO_PRODUCTS,
            )
            return
        }
        val composed =
            subscriptionEmailComposer.composeProductRecommendation(
                recipient = plan.email,
                basis = plan.basis,
                items = plan.items,
                unsubscribeUrl = unsubscribeUrl(plan.unsubscribeToken),
                surveyFallback = plan.surveyFallback,
            )
        val sent =
            runCatching {
                outboundMailPort.send(
                    OutboundMailMessage(
                        to = plan.email,
                        subject = composed.subject,
                        plainTextBody = composed.plainText,
                        htmlBody = composed.htmlBody,
                        headers = OutboundMailMessage.listUnsubscribeHeaders(oneClickUrl(plan.unsubscribeToken)),
                        kind = MailSendKind.PRODUCT_RECOMMENDATION,
                        dispatchId = dispatchId,
                    ),
                )
            }.onFailure { ex ->
                log.error("상품 추천 메일 발송 실패: subscriptionId={}", plan.subscriptionId, ex)
            }
        subscriptionDispatchTxService.recordOutcome(
            dispatchId = dispatchId,
            email = plan.email,
            sent = sent.isSuccess,
            error = sent.exceptionOrNull()?.message,
            sentAt = LocalDateTime.now(),
        )
        if (sent.isSuccess) {
            runCatching {
                productRecommendationMailDispatchTxService.recordSent(
                    subscriptionId = plan.subscriptionId,
                    productIds = plan.items.map { it.productId },
                    now = LocalDateTime.now(),
                )
            }.onFailure { ex ->
                log.warn(
                    "상품 추천 메일 발송 기록 저장 실패: subscriptionId={} cause={}",
                    plan.subscriptionId,
                    ex.javaClass.simpleName,
                )
            }
        }
    }

    private fun unsubscribeUrl(token: String): String =
        "${appPublicUrlProperties.frontendBaseUrl.trim().trimEnd('/')}$UNSUBSCRIBE_PATH?token=$token"

    private fun oneClickUrl(token: String): String? {
        val base = appPublicUrlProperties.publicBaseUrl.trim().trimEnd('/')
        if (base.isEmpty()) return null
        return "$base$ONE_CLICK_PATH?token=$token"
    }

    companion object {
        private val CONTENT_TYPE = SubscriptionContentType.PRODUCT_RECOMMENDATION
        private const val TITLE_PREFIX = "상품 추천 메일"
        private const val UNSUBSCRIBE_PATH = "/recommendation-mail/unsubscribe"
        private const val ONE_CLICK_PATH = "/recommendation-mail/unsubscribe/one-click"
    }
}
