package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.subscription.application.dto.ProductRecommendationMailConsentReminderPlan
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSchedule
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

@Component
class ProductRecommendationMailConsentReminderScheduler(
    private val appMailProperties: AppMailProperties,
    private val appPublicUrlProperties: AppPublicUrlProperties,
    private val productRecommendationMailConsentReminderTxService: ProductRecommendationMailConsentReminderTxService,
    private val productRecommendationMailNoticeComposer: ProductRecommendationMailNoticeComposer,
    private val outboundMailPort: OutboundMailPort,
) {
    private val log = LoggerFactory.getLogger(ProductRecommendationMailConsentReminderScheduler::class.java)
    internal var clock: Clock = Clock.system(IdolGlowDateTimes.ZONE)

    @Scheduled(
        cron = "\${subscription.recommendation-mail.consent-reminder-cron:0 0 10 * * *}",
        zone = "Asia/Seoul",
    )
    @SchedulerLock(
        name = "productRecommendationMailConsentReminder",
        lockAtLeastFor = "PT30S",
        lockAtMostFor = "PT30M",
    )
    fun remindDueConsents() {
        if (!appMailProperties.enabled) return
        val now = LocalDateTime.now(clock)
        if (now.hour !in SubscriptionDispatchSchedule.ADVERTISING_DISPATCH_HOURS) {
            log.info("상품 추천 메일 수신 동의 안내는 8시부터 20시 사이에만 보냅니다.")
            return
        }
        val plans = productRecommendationMailConsentReminderTxService.plan(now)
        val sent = plans.count(::sendOne)
        if (plans.isNotEmpty()) {
            log.info("상품 추천 메일 수신 동의 안내 발송: 대상 {}건, 성공 {}건", plans.size, sent)
        }
    }

    private fun sendOne(plan: ProductRecommendationMailConsentReminderPlan): Boolean {
        val sent =
            runCatching {
                val composed =
                    productRecommendationMailNoticeComposer.composeConsentReminder(
                        recipient = plan.email,
                        consentedAt = plan.consentedAt,
                        unsubscribeUrl = unsubscribeUrl(plan.unsubscribeToken),
                    )
                outboundMailPort.send(
                    OutboundMailMessage(
                        to = plan.email,
                        subject = composed.subject,
                        plainTextBody = composed.plainText,
                        htmlBody = composed.htmlBody,
                    ),
                )
            }.onFailure { ex ->
                log.warn(
                    "상품 추천 메일 수신 동의 안내 발송 실패: subscriptionId={} cause={}",
                    plan.subscriptionId,
                    ex.javaClass.simpleName,
                )
            }.isSuccess
        if (!sent) return false
        runCatching {
            productRecommendationMailConsentReminderTxService.recordReminded(
                subscriptionId = plan.subscriptionId,
                now = LocalDateTime.now(clock),
            )
        }.onFailure { ex ->
            log.warn(
                "상품 추천 메일 수신 동의 안내 기록 저장 실패: subscriptionId={} cause={}",
                plan.subscriptionId,
                ex.javaClass.simpleName,
            )
        }
        return true
    }

    private fun unsubscribeUrl(token: String): String =
        "${appPublicUrlProperties.frontendBaseUrl.trim().trimEnd('/')}$UNSUBSCRIBE_PATH?token=$token"

    companion object {
        private const val UNSUBSCRIBE_PATH = "/recommendation-mail/unsubscribe"
    }
}
