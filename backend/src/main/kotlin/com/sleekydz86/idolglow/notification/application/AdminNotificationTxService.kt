package com.sleekydz86.idolglow.notification.application

import com.sleekydz86.idolglow.notification.application.event.AdminNotificationRequestedEvent
import com.sleekydz86.idolglow.notification.application.port.out.AdminRecipientPort
import com.sleekydz86.idolglow.notification.domain.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class AdminNotificationTxService(
    private val adminRecipientPort: AdminRecipientPort,
    private val notificationRepository: NotificationRepository,
    private val notificationCommandService: NotificationCommandService,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun notifyAdmins(event: AdminNotificationRequestedEvent): Int =
        adminRecipientPort
            .findAdminUserIds()
            .filterNot { adminId -> event.deduplicateByLink && alreadyNotified(adminId, event) }.count { adminId ->
                notificationCommandService.create(
                    userId = adminId,
                    type = event.type,
                    title = event.title.take(TITLE_LIMIT),
                    message = event.message.take(MESSAGE_LIMIT),
                    link = event.link.take(LINK_LIMIT),
                ) != null
            }

    private fun alreadyNotified(
        adminId: Long,
        event: AdminNotificationRequestedEvent,
    ): Boolean = notificationRepository.existsByUserIdAndTypeAndLink(adminId, event.type, event.link)

    companion object {
        private const val TITLE_LIMIT = 120
        private const val MESSAGE_LIMIT = 500
        private const val LINK_LIMIT = 255
    }
}
