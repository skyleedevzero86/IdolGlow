package com.sleekydz86.idolglow.notification.infrastructure

import com.sleekydz86.idolglow.notification.domain.NotificationPreference
import com.sleekydz86.idolglow.notification.domain.NotificationType
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationPreferenceJpaRepository : JpaRepository<NotificationPreference, Long> {
    fun findAllByUserId(userId: Long): List<NotificationPreference>
    fun findByUserIdAndType(userId: Long, type: NotificationType): NotificationPreference?
    fun existsByUserIdAndTypeAndEnabledFalse(userId: Long, type: NotificationType): Boolean
}
