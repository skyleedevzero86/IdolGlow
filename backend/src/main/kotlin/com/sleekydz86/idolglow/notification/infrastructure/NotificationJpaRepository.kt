package com.sleekydz86.idolglow.notification.infrastructure

import com.sleekydz86.idolglow.notification.domain.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationJpaRepository : JpaRepository<Notification, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<Notification>
}