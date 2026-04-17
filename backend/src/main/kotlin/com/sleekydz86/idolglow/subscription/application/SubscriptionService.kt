package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionOverviewResponse
import com.sleekydz86.idolglow.newsletter.domain.Newsletter
import com.sleekydz86.idolglow.subscription.application.dto.RegisterSubscriptionCommand
import com.sleekydz86.idolglow.subscription.application.dto.SubscriptionRegistrationResponse
import com.sleekydz86.idolglow.subscription.application.dto.create
import com.sleekydz86.idolglow.subscription.application.dto.toRegistrationResponse
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionAdminUseCase
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionDispatchRecorder
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionPublicUseCase
import com.sleekydz86.idolglow.subscription.application.port.out.EmailSubscriptionPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchHistoryPort
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.SubscriptionAudience
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.webzine.domain.WebzineIssue
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional(readOnly = true)
@Service
class SubscriptionService(
    private val emailSubscriptionPort: EmailSubscriptionPort,
    private val subscriptionDispatchHistoryPort: SubscriptionDispatchHistoryPort,
) : SubscriptionPublicUseCase, SubscriptionAdminUseCase, SubscriptionDispatchRecorder {

    @Transactional
    override fun subscribe(command: RegisterSubscriptionCommand): SubscriptionRegistrationResponse {
        val normalizedEmail = command.email.trim().lowercase()
        require(normalizedEmail.isNotBlank()) { "구독 이메일은 비울 수 없습니다." }
        require(command.subscribeNewsletters || command.subscribeIssues) {
            "소식지 또는 웹진 호 중 하나 이상을 선택해야 합니다."
        }

        val now = LocalDateTime.now()
        val existing = emailSubscriptionPort.findByEmail(normalizedEmail)
        val saved = if (existing == null) {
            emailSubscriptionPort.save(
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
            emailSubscriptionPort.save(existing)
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

        val allSubscribers = emailSubscriptionPort.findAllByLatest()
        val allDispatches = subscriptionDispatchHistoryPort.findAllByLatest()

        val subscriberSlice = allSubscribers.slicePage(resolvedSubscriberPage, resolvedSubscriberSize)
        val dispatchSlice = allDispatches.slicePage(resolvedDispatchPage, resolvedDispatchSize)

        return AdminSubscriptionOverviewResponse.create(
            totalActive = emailSubscriptionPort.countActive(),
            newsletterSubscriberCount = emailSubscriptionPort.countActiveByAudience(SubscriptionAudience.NEWSLETTER),
            issueSubscriberCount = emailSubscriptionPort.countActiveByAudience(SubscriptionAudience.WEBZINE_ISSUE),
            totalDispatches = subscriptionDispatchHistoryPort.count(),
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
        if (subscriptionDispatchHistoryPort.existsByContentTypeAndContentSlug(
                contentType = SubscriptionContentType.NEWSLETTER,
                contentSlug = newsletter.slug,
            )
        ) {
            return
        }

        subscriptionDispatchHistoryPort.save(
            SubscriptionDispatchHistory.record(
                contentType = SubscriptionContentType.NEWSLETTER,
                contentSlug = newsletter.slug,
                contentTitle = newsletter.title,
                contentSummary = newsletter.summary,
                recipientCount = emailSubscriptionPort.countActiveByAudience(SubscriptionAudience.NEWSLETTER),
                contentCreatedAt = newsletter.createdAt,
                dispatchedAt = LocalDateTime.now(),
            )
        )
    }

    @Transactional
    override fun recordWebzineIssueDispatch(issue: WebzineIssue) {
        if (subscriptionDispatchHistoryPort.existsByContentTypeAndContentSlug(
                contentType = SubscriptionContentType.WEBZINE_ISSUE,
                contentSlug = issue.slug,
            )
        ) {
            return
        }

        subscriptionDispatchHistoryPort.save(
            SubscriptionDispatchHistory.record(
                contentType = SubscriptionContentType.WEBZINE_ISSUE,
                contentSlug = issue.slug,
                contentTitle = "Vol.${issue.volume} 호별보기",
                contentSummary = issue.teaser,
                recipientCount = emailSubscriptionPort.countActiveByAudience(SubscriptionAudience.WEBZINE_ISSUE),
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
