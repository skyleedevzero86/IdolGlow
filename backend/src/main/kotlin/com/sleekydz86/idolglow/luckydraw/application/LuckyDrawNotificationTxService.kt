package com.sleekydz86.idolglow.luckydraw.application

import com.sleekydz86.idolglow.luckydraw.application.event.LuckyDrawCompletedEvent
import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class LuckyDrawNotificationTxService(
    private val notificationCommandService: NotificationCommandService,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun notifyWinner(
        event: LuckyDrawCompletedEvent,
        userId: Long,
    ) {
        notificationCommandService.create(
            userId = userId,
            type = NotificationType.LUCKY_DRAW_WON,
            title = "응모에 당첨되었습니다",
            message = "${event.title} 응모에서 ${event.prizeName} 경품에 당첨되었습니다.",
            link = EVENT_DAY_LINK,
        )
    }

    companion object {
        private const val EVENT_DAY_LINK = "/event-day"
    }
}
