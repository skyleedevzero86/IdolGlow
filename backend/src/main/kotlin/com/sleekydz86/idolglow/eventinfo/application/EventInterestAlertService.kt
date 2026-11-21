package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventInterestPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaMessage
import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EventInterestAlertService(
    private val consumedEventGuard: ConsumedEventGuard,
    private val eventInterestPort: EventInterestPort,
    private val notificationCommandService: NotificationCommandService,
) {
    @Transactional
    fun handle(message: EventMetaMessage): EventMetaApplyResult =
        when {
            !consumedEventGuard.firstDelivery(CONSUMER, message.eventId) -> EventMetaApplyResult.DUPLICATE
            !message.alertWorthy() -> EventMetaApplyResult.IGNORED
            else -> {
                notifyInterestedUsers(message)
                EventMetaApplyResult.APPLIED
            }
        }

    private fun notifyInterestedUsers(message: EventMetaMessage) {
        val title = message.snapshot?.title.orEmpty()
        eventInterestPort.findUserIdsByGlobalId(message.globalId).forEach { userId ->
            notificationCommandService.create(
                userId = userId,
                type = NotificationType.EVENT_INTEREST_CHANGED,
                title = "관심 공연 정보가 바뀌었습니다",
                message = "$title 의 ${changeText(message)}이 바뀌었습니다.",
                link = "/event-info/${message.globalId}",
            )
        }
    }

    private fun changeText(message: EventMetaMessage): String =
        message.changedFields
            .mapNotNull { FIELD_TEXT[it] }
            .distinct()
            .joinToString("·")
            .ifEmpty { "정보" }

    companion object {
        const val CONSUMER: String = "event-meta-interest-alert"
        private val FIELD_TEXT: Map<String, String> =
            mapOf(
                "startDate" to "일정",
                "endDate" to "일정",
                "venue" to "장소",
                "status" to "상태",
            )
    }
}
