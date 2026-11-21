package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventInterestPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaApplyResult
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaMessage
import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.UriUtils

@Service
class EventInterestAlertService(
    private val consumedEventGuard: ConsumedEventGuard,
    private val eventInterestPort: EventInterestPort,
    private val eventSourceRefPort: EventSourceRefPort,
    private val notificationCommandService: NotificationCommandService,
) {
    @Transactional
    fun handle(message: EventMetaMessage): EventMetaApplyResult {
        val consumed =
            consumedEventGuard.firstDelivery(CONSUMER, message.eventId) ?: return EventMetaApplyResult.DUPLICATE
        val result =
            if (message.alertWorthy()) {
                notifyInterestedUsers(message)
                EventMetaApplyResult.APPLIED
            } else {
                EventMetaApplyResult.IGNORED
            }
        consumed.markResult(result)
        return result
    }

    private fun notifyInterestedUsers(message: EventMetaMessage) {
        val userIds = eventInterestPort.findUserIdsByGlobalId(message.globalId)
        if (userIds.isEmpty()) return
        val title = message.snapshot?.title.orEmpty()
        val link = detailLink(message.globalId)
        userIds.forEach { userId ->
            notificationCommandService.create(
                userId = userId,
                type = NotificationType.EVENT_INTEREST_CHANGED,
                title = "관심 공연 정보가 바뀌었습니다",
                message = changeMessage(title, changeText(message)),
                link = link,
            )
        }
    }

    private fun detailLink(globalId: String): String {
        val ref =
            eventSourceRefPort.findByGlobalId(globalId).firstOrNull { it.confirmedGlobalId() != null }
                ?: return EVENT_LIST_PATH
        return "$EVENT_LIST_PATH/${UriUtils.encodePathSegment(ref.source, Charsets.UTF_8)}/" +
            UriUtils.encodePathSegment(ref.sourceId, Charsets.UTF_8)
    }

    private fun changeMessage(
        title: String,
        changed: String,
    ): String = "${title}의 $changed${subjectParticle(changed)} 바뀌었습니다."

    private fun subjectParticle(word: String): String {
        val last = word.lastOrNull()
        val withoutFinalConsonant =
            last != null && last in HANGUL_FIRST..HANGUL_LAST && (last - HANGUL_FIRST) % JONGSEONG_COUNT == 0
        return if (withoutFinalConsonant) "가" else "이"
    }

    private fun changeText(message: EventMetaMessage): String =
        message.changedFields
            .mapNotNull { FIELD_TEXT[it] }
            .distinct()
            .joinToString("·")
            .ifEmpty { "정보" }

    companion object {
        const val CONSUMER: String = "event-meta-interest-alert"
        private const val EVENT_LIST_PATH: String = "/event-info"
        private const val HANGUL_FIRST: Char = '가'
        private const val HANGUL_LAST: Char = '힣'
        private const val JONGSEONG_COUNT: Int = 28
        private val FIELD_TEXT: Map<String, String> =
            mapOf(
                "startDate" to "일정",
                "endDate" to "일정",
                "venue" to "장소",
                "status" to "상태",
            )
    }
}
