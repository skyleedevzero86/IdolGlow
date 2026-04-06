package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.newsletter.domain.NewsletterRepository
import com.sleekydz86.idolglow.subscription.application.dto.RegisterSubscriptionCommand
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionDispatchRecorder
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionPublicUseCase
import com.sleekydz86.idolglow.subscription.application.port.out.EmailSubscriptionPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchHistoryPort
import com.sleekydz86.idolglow.webzine.domain.WebzineIssueRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local", "dev")
@DependsOn("newsletterAdminDataInitializer", "webzineAdminDataInitializer")
@Component
class SubscriptionAdminDataInitializer(
    private val subscriptionPublicUseCase: SubscriptionPublicUseCase,
    private val subscriptionDispatchRecorder: SubscriptionDispatchRecorder,
    private val emailSubscriptionPort: EmailSubscriptionPort,
    private val subscriptionDispatchHistoryPort: SubscriptionDispatchHistoryPort,
    private val newsletterRepository: NewsletterRepository,
    private val webzineIssueRepository: WebzineIssueRepository,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(SubscriptionAdminDataInitializer::class.java)

    override fun run(args: ApplicationArguments) {
        if (emailSubscriptionPort.count() == 0L) {
            seedSubscribers()
        }

        if (subscriptionDispatchHistoryPort.count() == 0L) {
            newsletterRepository.findAllByLatest()
                .take(2)
                .forEach(subscriptionDispatchRecorder::recordNewsletterDispatch)

            webzineIssueRepository.findAllByLatest()
                .take(2)
                .forEach(subscriptionDispatchRecorder::recordWebzineIssueDispatch)
        }

        log.info("Subscription sample data and dispatch histories are ready.")
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
}
