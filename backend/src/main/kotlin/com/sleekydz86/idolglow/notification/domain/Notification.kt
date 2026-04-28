package com.sleekydz86.idolglow.notification.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "notifications")
class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    val type: NotificationType,

    @Column(nullable = false, length = 120)
    val title: String,

    @Column(nullable = false, length = 500)
    val message: String,

    @Column(length = 255)
    val link: String? = null,

    @Column(name = "read_at")
    var readAt: LocalDateTime? = null
) : BaseEntity() {

    fun markRead(readAt: LocalDateTime = LocalDateTime.now()) {
        this.readAt = readAt
    }
}
