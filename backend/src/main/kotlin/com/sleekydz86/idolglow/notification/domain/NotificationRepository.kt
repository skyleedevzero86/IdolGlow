package com.sleekydz86.idolglow.notification.domain

interface NotificationRepository {
    fun save(notification: Notification): Notification
    fun findById(id: Long): Notification?
    fun findAllByUserId(userId: Long): List<Notification>
}