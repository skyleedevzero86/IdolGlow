package com.sleekydz86.idolglow.notification.infrastructure

import com.sleekydz86.idolglow.notification.domain.Notification
import com.sleekydz86.idolglow.notification.domain.NotificationRepository
import com.sleekydz86.idolglow.notification.domain.NotificationType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class NotificationRepositoryImpl(
    private val notificationJpaRepository: NotificationJpaRepository
) : NotificationRepository {

    override fun save(notification: Notification): Notification =
        notificationJpaRepository.save(notification)

    override fun findById(id: Long): Notification? =
        notificationJpaRepository.findByIdOrNull(id)

    override fun findAllByUserId(userId: Long): List<Notification> =
        notificationJpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId)

    override fun findVisibleByUserId(userId: Long, createdAtFrom: LocalDateTime): List<Notification> =
        notificationJpaRepository.findAllByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(userId, createdAtFrom)

    override fun findAllByUserIdAndType(userId: Long, type: NotificationType): List<Notification> =
        notificationJpaRepository.findAllByUserIdAndTypeOrderByCreatedAtDesc(userId, type)

    override fun countUnreadByUserId(userId: Long): Long =
        notificationJpaRepository.countByUserIdAndReadAtIsNull(userId)

    override fun countUnreadVisibleByUserId(userId: Long, createdAtFrom: LocalDateTime): Long =
        notificationJpaRepository.countByUserIdAndReadAtIsNullAndCreatedAtGreaterThanEqual(userId, createdAtFrom)

    override fun markAllReadByUserId(userId: Long, readAt: LocalDateTime) {
        notificationJpaRepository.markAllReadByUserId(userId, readAt)
    }

    override fun existsByUserIdAndTypeAndLink(userId: Long, type: NotificationType, link: String): Boolean =
        notificationJpaRepository.existsByUserIdAndTypeAndLink(userId, type, link)
}
