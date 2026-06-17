package com.sleekydz86.idolglow.subscription.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

enum class SubscriptionAudience(val label: String) {
    NEWSLETTER("소식지"),
    WEBZINE_ISSUE("호별보기"),
}
