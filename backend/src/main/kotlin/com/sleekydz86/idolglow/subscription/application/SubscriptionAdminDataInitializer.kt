package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowIssueRepository
import com.sleekydz86.idolglow.newsletter.domain.NewsletterRepository
import com.sleekydz86.idolglow.subscription.application.dto.RegisterSubscriptionCommand
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionDispatchRecorder
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionPublicUseCase
import com.sleekydz86.idolglow.subscription.application.port.out.EmailSubscriptionPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchHistoryPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchSchedulePort
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchFrequency
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSchedule
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.DayOfWeek

@Profile("local", "dev")
@DependsOn("newsletterAdminDataInitializer", "idolGlowAdminDataInitializer")
@Component
class SubscriptionAdminDataInitializer(
    private val subscriptionPublicUseCase: SubscriptionPublicUseCase,
    private val subscriptionDispatchRecorder: SubscriptionDispatchRecorder,
    private val emailSubscriptionPort: EmailSubscriptionPort,
    private val subscriptionDispatchHistoryPort: SubscriptionDispatchHistoryPort,
    private val newsletterRepository: NewsletterRepository,
    private val idolglowIssueRepository: IdolGlowIssueRepository,
    private val subscriptionDispatchSchedulePort: SubscriptionDispatchSchedulePort,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(SubscriptionAdminDataInitializer::class.java)

    override fun run(args: ApplicationArguments) {
        if (emailSubscriptionPort.count() == 0L) {
            seedSubscribers()
        }

        if (subscriptionDispatchHistoryPort.count() == 0L) {
            newsletterRepository
                .findAllByLatest()
                .take(2)
                .forEach(subscriptionDispatchRecorder::recordNewsletterDispatch)

            idolglowIssueRepository
                .findAllByLatest()
                .take(2)
                .forEach(subscriptionDispatchRecorder::recordIdolGlowIssueDispatch)
        }

        seedProductRecommendationSchedule()

        log.info("구독 샘플 데이터와 발송 이력 준비가 완료되었습니다.")
    }

    private fun seedProductRecommendationSchedule() {
        val contentType = SubscriptionContentType.PRODUCT_RECOMMENDATION
        if (subscriptionDispatchSchedulePort.findByContentType(contentType) != null) return
        subscriptionDispatchSchedulePort.save(
            SubscriptionDispatchSchedule.create(
                contentType = contentType,
                frequencyType = SubscriptionDispatchFrequency.WEEKLY,
                dayOfWeek = DayOfWeek.MONDAY,
                dispatchHour = PRODUCT_RECOMMENDATION_DISPATCH_HOUR,
                dispatchMinute = 0,
                active = false,
            ),
        )
    }

    private fun seedSubscribers() {
        listOf(
            RegisterSubscriptionCommand(
                email = "editor@idolglow.local",
                subscribeNewsletters = true,
                subscribeIssues = true,
                source = "LOCAL_SEED",
            ),
            RegisterSubscriptionCommand(
                email = "curation@idolglow.local",
                subscribeNewsletters = false,
                subscribeIssues = true,
                source = "LOCAL_SEED",
            ),
            RegisterSubscriptionCommand(
                email = "newsletter@idolglow.local",
                subscribeNewsletters = true,
                subscribeIssues = false,
                source = "LOCAL_SEED",
            ),
        ).forEach(subscriptionPublicUseCase::subscribe)
    }

    companion object {
        private const val PRODUCT_RECOMMENDATION_DISPATCH_HOUR = 10
    }
}
