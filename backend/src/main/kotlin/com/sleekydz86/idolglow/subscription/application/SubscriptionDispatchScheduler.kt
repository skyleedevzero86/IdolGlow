package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.newsletter.domain.NewsletterRepository
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionDispatchRecorder
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchSchedulePort
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.webzine.domain.WebzineIssueRepository
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
    private val webzineIssueRepository: WebzineIssueRepository,
) {

    private val lastTriggeredMinutes = ConcurrentHashMap<Long, LocalDateTime>()

    @Scheduled(fixedDelayString = "\${subscription.dispatch.scheduler-interval-ms:30000}")
    fun dispatchScheduledContents() {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)

        subscriptionDispatchSchedulePort.findAllActive()
            .filter { schedule -> schedule.shouldTriggerAt(now) }
            .filterNot { schedule -> lastTriggeredMinutes[schedule.id] == now }
            .forEach { schedule ->
                lastTriggeredMinutes[schedule.id] = now
                dispatchLatestContent(schedule.contentType)
            }
    }

    private fun dispatchLatestContent(contentType: SubscriptionContentType) {
        when (contentType) {
            SubscriptionContentType.NEWSLETTER ->
                newsletterRepository.findAllByLatest().firstOrNull()?.let(subscriptionDispatchRecorder::recordNewsletterDispatch)

            SubscriptionContentType.WEBZINE_ISSUE ->
                webzineIssueRepository.findAllByLatest().firstOrNull()?.let(subscriptionDispatchRecorder::recordWebzineIssueDispatch)
        }
    }
}
