package com.sleekydz86.idolglow.notification.application

import com.sleekydz86.idolglow.notification.application.dto.NotificationPreferenceResponse
import com.sleekydz86.idolglow.notification.domain.NotificationPreference
import com.sleekydz86.idolglow.notification.domain.NotificationPreferenceRepository
import com.sleekydz86.idolglow.notification.domain.NotificationType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationPreferenceService(
    private val notificationPreferenceRepository: NotificationPreferenceRepository
) {

    @Transactional(readOnly = true)
    fun findAll(userId: Long): List<NotificationPreferenceResponse> {
        val saved = notificationPreferenceRepository.findAllByUserId(userId)
            .associateBy { it.type }
        return NotificationType.entries.map { type ->
            val pref = saved[type]
            NotificationPreferenceResponse(type = type, enabled = pref?.enabled ?: true)
        }
    }

    @Transactional
    fun update(userId: Long, type: NotificationType, enabled: Boolean): NotificationPreferenceResponse {
        val preference = notificationPreferenceRepository.findByUserIdAndType(userId, type)
            ?.also { it.update(enabled) }
            ?: notificationPreferenceRepository.save(
                NotificationPreference(userId = userId, type = type, enabled = enabled)
            )
        return NotificationPreferenceResponse(type = preference.type, enabled = preference.enabled)
    }
}
