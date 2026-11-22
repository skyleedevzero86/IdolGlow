package com.sleekydz86.idolglow.subscription.application.port.`in`

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetPageResult
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

interface SubscriptionAdminQueryUseCase {
    fun findEmailSubscribers(
        condition: EmailSubscriberSearchCondition,
        request: OffsetPageRequest,
    ): OffsetPageResult<AdminEmailSubscriberResult>

    fun findRecommendationSubscribers(
        condition: RecommendationSubscriberSearchCondition,
        request: OffsetPageRequest,
    ): OffsetPageResult<AdminRecommendationSubscriberResult>

    fun findDispatches(
        condition: DispatchHistorySearchCondition,
        request: OffsetPageRequest,
    ): OffsetPageResult<SubscriptionDispatchResult>

    fun findDispatchDetail(
        condition: DispatchRecipientSearchCondition,
        request: OffsetPageRequest,
    ): AdminSubscriptionDispatchDetailResult

    fun findMailLogs(
        condition: MailSendLogSearchCondition,
        request: OffsetPageRequest,
    ): OffsetPageResult<AdminMailSendLogResult>

    fun findMailLog(id: Long): AdminMailSendLogDetailResult
}
