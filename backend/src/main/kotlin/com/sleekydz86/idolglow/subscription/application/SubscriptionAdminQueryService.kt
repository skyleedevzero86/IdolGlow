package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.subscription.SubscriptionAdminExceptionType
import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
import com.sleekydz86.idolglow.subscription.application.dto.AdminDispatchMailContentResult
import com.sleekydz86.idolglow.subscription.application.dto.AdminDispatchRecipientResult
import com.sleekydz86.idolglow.subscription.application.dto.AdminEmailSubscriberResult
import com.sleekydz86.idolglow.subscription.application.dto.AdminMailSendLogDetailResult
import com.sleekydz86.idolglow.subscription.application.dto.AdminMailSendLogResult
import com.sleekydz86.idolglow.subscription.application.dto.AdminRecommendationSubscriberResult
import com.sleekydz86.idolglow.subscription.application.dto.AdminSubscriptionDispatchDetailResult
import com.sleekydz86.idolglow.subscription.application.dto.DispatchHistorySearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.DispatchRecipientSearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.EmailSubscriberSearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.MailSendLogSearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.RecommendationSubscriberSearchCondition
import com.sleekydz86.idolglow.subscription.application.dto.SubscriptionDispatchResult
import com.sleekydz86.idolglow.subscription.application.dto.asSubscriptionDateTime
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionAdminQueryUseCase
import com.sleekydz86.idolglow.subscription.application.port.out.MailSendLogPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionAdminQueryPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchHistoryPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchRecipientPort
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipientCounts
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class SubscriptionAdminQueryService(
    private val subscriptionAdminQueryPort: SubscriptionAdminQueryPort,
    private val subscriptionDispatchHistoryPort: SubscriptionDispatchHistoryPort,
    private val subscriptionDispatchRecipientPort: SubscriptionDispatchRecipientPort,
    private val mailSendLogPort: MailSendLogPort,
) : SubscriptionAdminQueryUseCase {
    override fun findEmailSubscribers(
        condition: EmailSubscriberSearchCondition,
        request: OffsetPageRequest,
    ): OffsetPageResult<AdminEmailSubscriberResult> {
        val normalized = request.normalized()
        val slice = subscriptionAdminQueryPort.searchEmailSubscribers(condition, normalized)
        return OffsetPageResult.of(slice.items.map(AdminEmailSubscriberResult::from), normalized, slice.totalElements)
    }

    override fun findRecommendationSubscribers(
        condition: RecommendationSubscriberSearchCondition,
        request: OffsetPageRequest,
    ): OffsetPageResult<AdminRecommendationSubscriberResult> {
        val normalized = request.normalized()
        val slice = subscriptionAdminQueryPort.searchRecommendationSubscribers(condition, normalized)
        return OffsetPageResult.of(
            slice.items.map(AdminRecommendationSubscriberResult::from),
            normalized,
            slice.totalElements,
        )
    }

    override fun findDispatches(
        condition: DispatchHistorySearchCondition,
        request: OffsetPageRequest,
    ): OffsetPageResult<SubscriptionDispatchResult> {
        val normalized = request.normalized()
        val slice = subscriptionAdminQueryPort.searchDispatches(condition, normalized)
        val counts = subscriptionDispatchRecipientPort.countByDispatchIds(slice.items.map { it.id })
        val items = slice.items.map { history -> SubscriptionDispatchResult.from(history, counts.of(history.id)) }
        return OffsetPageResult.of(items, normalized, slice.totalElements)
    }

    override fun findDispatchDetail(
        condition: DispatchRecipientSearchCondition,
        request: OffsetPageRequest,
    ): AdminSubscriptionDispatchDetailResult {
        val history =
            subscriptionDispatchHistoryPort.findById(condition.dispatchId)
                ?: throw CustomException(SubscriptionAdminExceptionType.DISPATCH_NOT_FOUND)
        val normalized = request.normalized()
        val counts = subscriptionDispatchRecipientPort.countByDispatchIds(listOf(history.id))
        val recipients = subscriptionAdminQueryPort.searchDispatchRecipients(condition, normalized)
        return AdminSubscriptionDispatchDetailResult(
            dispatch = SubscriptionDispatchResult.from(history, counts.of(history.id)),
            lastAttemptAt = history.lastAttemptAt?.asSubscriptionDateTime(),
            recipients =
                OffsetPageResult.of(
                    recipients.items.map(AdminDispatchRecipientResult::from),
                    normalized,
                    recipients.totalElements,
                ),
            mailContent = mailSendLogPort.findLatestByDispatchId(history.id)?.let(AdminDispatchMailContentResult::from),
        )
    }

    override fun findMailLogs(
        condition: MailSendLogSearchCondition,
        request: OffsetPageRequest,
    ): OffsetPageResult<AdminMailSendLogResult> {
        val normalized = request.normalized()
        val slice = mailSendLogPort.search(condition, normalized)
        return OffsetPageResult.of(slice.items.map(AdminMailSendLogResult::from), normalized, slice.totalElements)
    }

    override fun findMailLog(id: Long): AdminMailSendLogDetailResult =
        mailSendLogPort.findById(id)?.let(AdminMailSendLogDetailResult::from)
            ?: throw CustomException(SubscriptionAdminExceptionType.MAIL_LOG_NOT_FOUND)

    private fun OffsetPageRequest.normalized(): OffsetPageRequest = OffsetPageRequest.of(page, size)

    private fun Map<Long, SubscriptionDispatchRecipientCounts>.of(id: Long): SubscriptionDispatchRecipientCounts =
        this[id] ?: SubscriptionDispatchRecipientCounts.EMPTY
}
