package com.sleekydz86.idolglow.notification.domain

interface NotificationRepository {
    fun save(notification: Notification): Notification
    fun findById(id: Long): Notification?
    fun findAllByUserId(userId: Long): List<Notification>
    fun findAllByUserIdAndType(userId: Long, type: NotificationType): List<Notification>
    fun countUnreadByUserId(userId: Long): Long
    fun markAllReadByUserId(userId: Long, readAt: java.time.LocalDateTime)
    fun existsByUserIdAndTypeAndLink(userId: Long, type: NotificationType, link: String): Boolean
}
