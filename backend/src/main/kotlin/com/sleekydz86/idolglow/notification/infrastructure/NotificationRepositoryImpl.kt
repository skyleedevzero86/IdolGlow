package com.sleekydz86.idolglow.notification.infrastructure

import com.sleekydz86.idolglow.notification.domain.Notification
import com.sleekydz86.idolglow.notification.domain.NotificationRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

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
}