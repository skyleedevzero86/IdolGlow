package com.sleekydz86.idolglow.notification.domain

import com.sleekydz86.idolglow.global.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "notification_preferences",
    uniqueConstraints = [UniqueConstraint(name = "uk_np_user_type", columnNames = ["user_id", "type"])]
)
class NotificationPreference(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    val type: NotificationType,

    @Column(nullable = false)
    var enabled: Boolean = true,
) : BaseEntity() {

    fun update(enabled: Boolean) {
        this.enabled = enabled
    }
}
