package com.sleekydz86.idolglow.luckydraw.application

import com.sleekydz86.idolglow.luckydraw.application.event.LuckyDrawCompletedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class LuckyDrawWinnerNotificationListener(
    private val luckyDrawNotificationTxService: LuckyDrawNotificationTxService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: LuckyDrawCompletedEvent) {
        event.winnerUserIds.forEach { userId ->
            runCatching { luckyDrawNotificationTxService.notifyWinner(event, userId) }
                .onFailure { log.warn("응모 당첨 알림 실패 drawId={} userId={}", event.drawId, userId, it) }
        }
    }
}
