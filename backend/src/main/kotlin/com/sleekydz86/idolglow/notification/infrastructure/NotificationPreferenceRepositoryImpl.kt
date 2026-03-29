package com.sleekydz86.idolglow.notification.infrastructure

import com.sleekydz86.idolglow.notification.domain.NotificationPreference
import com.sleekydz86.idolglow.notification.domain.NotificationPreferenceRepository
import com.sleekydz86.idolglow.notification.domain.NotificationType
import org.springframework.stereotype.Repository

@Repository
class NotificationPreferenceRepositoryImpl(
    private val notificationPreferenceJpaRepository: NotificationPreferenceJpaRepository
) : NotificationPreferenceRepository {

    override fun findAllByUserId(userId: Long): List<NotificationPreference> =
        notificationPreferenceJpaRepository.findAllByUserId(userId)

    override fun findByUserIdAndType(userId: Long, type: NotificationType): NotificationPreference? =
        notificationPreferenceJpaRepository.findByUserIdAndType(userId, type)

    override fun save(preference: NotificationPreference): NotificationPreference =
        notificationPreferenceJpaRepository.save(preference)

    override fun isDisabled(userId: Long, type: NotificationType): Boolean =
        notificationPreferenceJpaRepository.existsByUserIdAndTypeAndEnabledFalse(userId, type)
}
