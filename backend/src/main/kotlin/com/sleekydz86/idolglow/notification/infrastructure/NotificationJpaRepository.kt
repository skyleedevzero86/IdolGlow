package com.sleekydz86.idolglow.notification.infrastructure

import com.sleekydz86.idolglow.notification.domain.Notification
import com.sleekydz86.idolglow.notification.domain.NotificationType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface NotificationJpaRepository : JpaRepository<Notification, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<Notification>
    fun findAllByUserIdAndTypeOrderByCreatedAtDesc(userId: Long, type: NotificationType): List<Notification>
    fun countByUserIdAndReadAtIsNull(userId: Long): Long
    fun existsByUserIdAndTypeAndLink(userId: Long, type: NotificationType, link: String): Boolean

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.userId = :userId AND n.readAt IS NULL")
    fun markAllReadByUserId(userId: Long, readAt: LocalDateTime): Int
}
