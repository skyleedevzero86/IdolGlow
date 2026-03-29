package com.sleekydz86.idolglow.notification.domain

interface NotificationPreferenceRepository {
    fun findAllByUserId(userId: Long): List<NotificationPreference>
    fun findByUserIdAndType(userId: Long, type: NotificationType): NotificationPreference?
    fun save(preference: NotificationPreference): NotificationPreference
    fun isDisabled(userId: Long, type: NotificationType): Boolean
}
