package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowIssueRepository
import com.sleekydz86.idolglow.newsletter.domain.NewsletterRepository
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionDispatchRecorder
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchHistoryPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchSchedulePort
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSchedule
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

@Component
class SubscriptionDispatchScheduler(
    private val subscriptionDispatchSchedulePort: SubscriptionDispatchSchedulePort,
    private val subscriptionDispatchRecorder: SubscriptionDispatchRecorder,
    private val newsletterRepository: NewsletterRepository,
    private val idolglowIssueRepository: IdolGlowIssueRepository,
    private val subscriptionDispatchHistoryPort: SubscriptionDispatchHistoryPort,
    private val appMailProperties: AppMailProperties,
    private val productRecommendationMailDispatcher: ProductRecommendationMailDispatcher,
) {
    private val log = LoggerFactory.getLogger(SubscriptionDispatchScheduler::class.java)
    private val lastTriggeredMinutes = ConcurrentHashMap<Long, LocalDateTime>()

    @Scheduled(fixedDelayString = "\${subscription.dispatch.scheduler-interval-ms:30000}")
    fun dispatchScheduledContents() {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)

        subscriptionDispatchSchedulePort
            .findAllActive()
            .filter { schedule -> schedule.shouldTriggerAt(now) }
            .filterNot { schedule -> lastTriggeredMinutes[schedule.id] == now }
            .forEach { schedule ->
                lastTriggeredMinutes[schedule.id] = now
                dispatchLatestContent(schedule)
            }
    }

    private fun dispatchLatestContent(schedule: SubscriptionDispatchSchedule) {
        when (schedule.contentType) {
            SubscriptionContentType.NEWSLETTER ->
                newsletterRepository.findAllByLatest().firstOrNull()?.let(subscriptionDispatchRecorder::recordNewsletterDispatch)

            SubscriptionContentType.IDOLGLOW_ISSUE ->
                idolglowIssueRepository.findAllByLatest().firstOrNull()?.let(subscriptionDispatchRecorder::recordIdolGlowIssueDispatch)

            SubscriptionContentType.PRODUCT_RECOMMENDATION ->
                productRecommendationMailDispatcher.dispatch(schedule.frequencyType)
        }
    }

    @Scheduled(fixedDelayString = "\${subscription.dispatch.scheduler-interval-ms:30000}")
    fun resumeStaleDispatches() {
        if (!appMailProperties.enabled) {
            return
        }
        val staleBefore = LocalDateTime.now().minusMinutes(SubscriptionDispatchHistory.RESUME_AFTER_MINUTES)
        subscriptionDispatchHistoryPort.findStaleSending(staleBefore).forEach(::republish)
    }

    private fun republish(history: SubscriptionDispatchHistory) {
        when (history.contentType) {
            SubscriptionContentType.NEWSLETTER ->
                newsletterRepository.findBySlug(history.contentSlug)?.let { newsletter ->
                    log.info(
                        "멈춘 구독 발송을 이어서 보냅니다. type={} slug={}",
                        history.contentType,
                        history.contentSlug,
                    )
                    subscriptionDispatchRecorder.recordNewsletterDispatch(newsletter)
                } ?: log.warn(
                    "멈춘 구독 발송의 콘텐츠를 찾지 못했습니다. type={} slug={}",
                    history.contentType,
                    history.contentSlug,
                )

            SubscriptionContentType.IDOLGLOW_ISSUE ->
                idolglowIssueRepository.findBySlug(history.contentSlug)?.let { issue ->
                    log.info(
                        "멈춘 구독 발송을 이어서 보냅니다. type={} slug={}",
                        history.contentType,
                        history.contentSlug,
                    )
                    subscriptionDispatchRecorder.recordIdolGlowIssueDispatch(issue)
                } ?: log.warn(
                    "멈춘 구독 발송의 콘텐츠를 찾지 못했습니다. type={} slug={}",
                    history.contentType,
                    history.contentSlug,
                )

            SubscriptionContentType.PRODUCT_RECOMMENDATION -> {
                log.info(
                    "멈춘 구독 발송을 이어서 보냅니다. type={} slug={}",
                    history.contentType,
                    history.contentSlug,
                )
                productRecommendationMailDispatcher.resume(history.contentSlug)
            }
        }
    }
}
