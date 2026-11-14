package com.sleekydz86.idolglow.notification.application

import com.sleekydz86.idolglow.notification.application.event.AdminNotificationRequestedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AdminNotificationEventListener(
    private val adminNotificationTxService: AdminNotificationTxService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun handle(event: AdminNotificationRequestedEvent) {
        runCatching { adminNotificationTxService.notifyAdmins(event) }
            .onFailure { log.warn("관리자 알림 생성 실패 type={} error={}", event.type, it.message) }
    }
}
