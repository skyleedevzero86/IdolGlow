package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionOverviewResponse
import com.sleekydz86.idolglow.newsletter.domain.Newsletter
import com.sleekydz86.idolglow.subscription.application.dto.RegisterSubscriptionCommand
import com.sleekydz86.idolglow.subscription.application.dto.SubscriptionRegistrationResponse
import com.sleekydz86.idolglow.subscription.application.dto.create
import com.sleekydz86.idolglow.subscription.application.dto.toRegistrationResponse
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.EmailSubscriptionRepository
import com.sleekydz86.idolglow.subscription.domain.SubscriptionAudience
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistoryRepository
import com.sleekydz86.idolglow.webzine.domain.WebzineIssue
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional(readOnly = true)
@Service
class SubscriptionService(
    private val emailSubscriptionRepository: EmailSubscriptionRepository,
    private val subscriptionDispatchHistoryRepository: SubscriptionDispatchHistoryRepository,
) : SubscriptionPublicUseCase, SubscriptionAdminUseCase, SubscriptionDispatchRecorder {

    @Transactional
    override fun subscribe(command: RegisterSubscriptionCommand): SubscriptionRegistrationResponse {
        val normalizedEmail = command.email.trim().lowercase()
        require(normalizedEmail.isNotBlank()) { "Subscription email must not be blank." }
        require(command.subscribeNewsletters || command.subscribeIssues) {
            "At least one subscription target must be selected."
        }

        val now = LocalDateTime.now()
        val existing = emailSubscriptionRepository.findByEmail(normalizedEmail)
        val saved = if (existing == null) {
            emailSubscriptionRepository.save(
                EmailSubscription.create(
                    email = normalizedEmail,
                    subscribedNewsletters = command.subscribeNewsletters,
                    subscribedIssues = command.subscribeIssues,
                    consentedAt = now,
                    subscribedAt = now,
                    subscriptionSource = command.source,
                )
            )
        } else {
            existing.resubscribe(
                subscribedNewsletters = command.subscribeNewsletters,
                subscribedIssues = command.subscribeIssues,
                consentedAt = now,
                subscribedAt = now,
                subscriptionSource = command.source,
            )
            emailSubscriptionRepository.save(existing)
        }

        return saved.toRegistrationResponse()
    }

    override fun findOverview(
        subscriberPage: Int,
        subscriberSize: Int,
        dispatchPage: Int,
        dispatchSize: Int,
    ): AdminSubscriptionOverviewResponse {
        val resolvedSubscriberPage = subscriberPage.coerceAtLeast(1)
        val resolvedSubscriberSize = subscriberSize.coerceIn(1, 20)
        val resolvedDispatchPage = dispatchPage.coerceAtLeast(1)
        val resolvedDispatchSize = dispatchSize.coerceIn(1, 20)

        val allSubscribers = emailSubscriptionRepository.findAllByLatest()
        val allDispatches = subscriptionDispatchHistoryRepository.findAllByLatest()

        val subscriberSlice = allSubscribers.slicePage(resolvedSubscriberPage, resolvedSubscriberSize)
        val dispatchSlice = allDispatches.slicePage(resolvedDispatchPage, resolvedDispatchSize)

        return AdminSubscriptionOverviewResponse.create(
            totalActive = emailSubscriptionRepository.countActive(),
            newsletterSubscriberCount = emailSubscriptionRepository.countActiveByAudience(SubscriptionAudience.NEWSLETTER),
            issueSubscriberCount = emailSubscriptionRepository.countActiveByAudience(SubscriptionAudience.WEBZINE_ISSUE),
            totalDispatches = subscriptionDispatchHistoryRepository.count(),
            subscribers = subscriberSlice.items,
            subscriberPage = resolvedSubscriberPage,
            subscriberSize = resolvedSubscriberSize,
            subscriberTotalElements = allSubscribers.size.toLong(),
            subscriberTotalPages = subscriberSlice.totalPages,
            subscriberHasNext = subscriberSlice.hasNext,
            dispatches = dispatchSlice.items,
            dispatchPage = resolvedDispatchPage,
            dispatchSize = resolvedDispatchSize,
            dispatchTotalElements = allDispatches.size.toLong(),
            dispatchTotalPages = dispatchSlice.totalPages,
            dispatchHasNext = dispatchSlice.hasNext,
        )
    }

    @Transactional
    override fun recordNewsletterDispatch(newsletter: Newsletter) {
        if (subscriptionDispatchHistoryRepository.existsByContentTypeAndContentSlug(
                contentType = SubscriptionContentType.NEWSLETTER,
                contentSlug = newsletter.slug,
            )
        ) {
            return
        }

        subscriptionDispatchHistoryRepository.save(
            SubscriptionDispatchHistory.record(
                contentType = SubscriptionContentType.NEWSLETTER,
                contentSlug = newsletter.slug,
                contentTitle = newsletter.title,
                contentSummary = newsletter.summary,
                recipientCount = emailSubscriptionRepository.countActiveByAudience(SubscriptionAudience.NEWSLETTER),
                contentCreatedAt = newsletter.createdAt,
                dispatchedAt = LocalDateTime.now(),
            )
        )
    }

    @Transactional
    override fun recordWebzineIssueDispatch(issue: WebzineIssue) {
        if (subscriptionDispatchHistoryRepository.existsByContentTypeAndContentSlug(
                contentType = SubscriptionContentType.WEBZINE_ISSUE,
                contentSlug = issue.slug,
            )
        ) {
            return
        }

        subscriptionDispatchHistoryRepository.save(
            SubscriptionDispatchHistory.record(
                contentType = SubscriptionContentType.WEBZINE_ISSUE,
                contentSlug = issue.slug,
                contentTitle = "Vol.${issue.volume} 호별보기",
                contentSummary = issue.teaser,
                recipientCount = emailSubscriptionRepository.countActiveByAudience(SubscriptionAudience.WEBZINE_ISSUE),
                contentCreatedAt = issue.createdAt,
                dispatchedAt = LocalDateTime.now(),
            )
        )
    }

    private fun <T> List<T>.slicePage(page: Int, size: Int): PageSlice<T> {
        val fromIndex = ((page - 1) * size).coerceAtMost(this.size)
        val toIndex = (fromIndex + size).coerceAtMost(this.size)
        val totalElements = this.size.toLong()
        val totalPages = if (totalElements == 0L) 0 else ((totalElements + size - 1) / size).toInt()

        return PageSlice(
            items = subList(fromIndex, toIndex),
            totalPages = totalPages,
            hasNext = toIndex < this.size,
        )
    }

    private data class PageSlice<T>(
        val items: List<T>,
        val totalPages: Int,
        val hasNext: Boolean,
    )
}
