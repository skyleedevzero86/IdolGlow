package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionLatestContentResponse
import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionOverviewResponse
import com.sleekydz86.idolglow.admin.ui.dto.AdminSubscriptionScheduleResponse
import com.sleekydz86.idolglow.newsletter.domain.Newsletter
import com.sleekydz86.idolglow.newsletter.domain.NewsletterRepository
import com.sleekydz86.idolglow.subscription.application.dto.RegisterSubscriptionCommand
import com.sleekydz86.idolglow.subscription.application.dto.SubscriptionRegistrationResponse
import com.sleekydz86.idolglow.subscription.application.dto.UpsertSubscriptionDispatchScheduleCommand
import com.sleekydz86.idolglow.subscription.application.dto.create
import com.sleekydz86.idolglow.subscription.application.dto.toRegistrationResponse
import com.sleekydz86.idolglow.subscription.application.event.NewsletterDispatchRequestedEvent
import com.sleekydz86.idolglow.subscription.application.event.WebzineIssueDispatchRequestedEvent
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionAdminUseCase
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionDispatchRecorder
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionPublicUseCase
import com.sleekydz86.idolglow.subscription.application.port.out.EmailSubscriptionPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchHistoryPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchSchedulePort
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.SubscriptionAudience
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSchedule
import com.sleekydz86.idolglow.webzine.domain.WebzineIssue
import com.sleekydz86.idolglow.webzine.domain.WebzineIssueRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Transactional(readOnly = true)
@Service
class SubscriptionService(
    private val emailSubscriptionPort: EmailSubscriptionPort,
    private val subscriptionDispatchHistoryPort: SubscriptionDispatchHistoryPort,
    private val subscriptionDispatchSchedulePort: SubscriptionDispatchSchedulePort,
    private val newsletterRepository: NewsletterRepository,
    private val webzineIssueRepository: WebzineIssueRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : SubscriptionPublicUseCase, SubscriptionAdminUseCase, SubscriptionDispatchRecorder {

    @Transactional
    override fun subscribe(command: RegisterSubscriptionCommand): SubscriptionRegistrationResponse {
        val normalizedEmail = command.email.trim().lowercase()
        require(normalizedEmail.isNotBlank()) { "구독 이메일은 비워둘 수 없습니다." }
        require(command.subscribeNewsletters || command.subscribeIssues) {
            "뉴스레터 또는 호별보기 중 하나 이상은 선택해야 합니다."
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
        val allSchedules = subscriptionDispatchSchedulePort.findAllByLatest()

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
            schedules = allSchedules,
            latestContents = latestContents(),
        )
    }

    @Transactional
    override fun upsertDispatchSchedule(command: UpsertSubscriptionDispatchScheduleCommand): AdminSubscriptionScheduleResponse {
        val dispatchTime = LocalTime.parse(command.dispatchTime.trim())
        val existing = subscriptionDispatchSchedulePort.findByContentType(command.contentType)

        val saved = if (existing == null) {
            subscriptionDispatchSchedulePort.save(
                SubscriptionDispatchSchedule.create(
                    contentType = command.contentType,
                    frequencyType = command.frequencyType,
                    dayOfWeek = command.dayOfWeek,
                    dispatchHour = dispatchTime.hour,
                    dispatchMinute = dispatchTime.minute,
                    active = command.active,
                )
            )
        } else {
            existing.update(
                frequencyType = command.frequencyType,
                dayOfWeek = command.dayOfWeek,
                dispatchHour = dispatchTime.hour,
                dispatchMinute = dispatchTime.minute,
                active = command.active,
            )
            subscriptionDispatchSchedulePort.save(existing)
        }

        return AdminSubscriptionScheduleResponse.from(saved)
    }

    @Transactional
    override fun recordNewsletterDispatch(newsletter: Newsletter) {
        applicationEventPublisher.publishEvent(
            NewsletterDispatchRequestedEvent(
                slug = newsletter.slug,
                title = newsletter.title,
                summary = newsletter.summary,
                imageUrl = newsletter.imageUrl,
                publishedAt = newsletter.publishedAt,
                tags = newsletter.tags.map { it.tagName },
                paragraphs = newsletter.paragraphs.map { it.body },
                contentCreatedAt = newsletter.createdAt,
            )
        )
    }

    @Transactional
    override fun recordWebzineIssueDispatch(issue: WebzineIssue) {
        applicationEventPublisher.publishEvent(
            WebzineIssueDispatchRequestedEvent(
                slug = issue.slug,
                volume = issue.volume,
                issueDate = issue.issueDate,
                teaser = issue.teaser,
                coverImageUrl = issue.coverImageUrl,
                articleTitles = issue.articles
                    .sortedByDescending { it.createdAt ?: LocalDateTime.MIN }
                    .map { it.title },
                contentCreatedAt = issue.createdAt,
            )
        )
    }

    private fun latestContents(): List<AdminSubscriptionLatestContentResponse> =
        buildList {
            newsletterRepository.findAllByLatest().firstOrNull()?.let { newsletter ->
                add(
                    AdminSubscriptionLatestContentResponse(
                        contentType = "NEWSLETTER",
                        contentTypeLabel = "뉴스레터",
                        title = newsletter.title,
                        slug = newsletter.slug,
                        summary = newsletter.summary,
                        publishedAt = newsletter.publishedAt.asDisplayDate(),
                    )
                )
            }

            webzineIssueRepository.findAllByLatest().firstOrNull()?.let { issue ->
                add(
                    AdminSubscriptionLatestContentResponse(
                        contentType = "WEBZINE_ISSUE",
                        contentTypeLabel = "호별보기",
                        title = "Vol.${issue.volume} 호별보기",
                        slug = issue.slug,
                        summary = issue.teaser,
                        publishedAt = issue.issueDate.asDisplayDate(),
                    )
                )
            }
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

private val subscriptionDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

private fun LocalDate.asDisplayDate(): String = format(subscriptionDateFormatter)
