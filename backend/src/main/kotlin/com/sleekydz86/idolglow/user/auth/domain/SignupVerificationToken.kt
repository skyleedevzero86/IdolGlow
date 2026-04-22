package com.sleekydz86.idolglow.user.auth.domain

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
@Table(name = "signup_verification_tokens")
class SignupVerificationToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false, unique = true, length = 120)
    val token: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    val type: SignupVerificationType,
    @Column(nullable = false, length = 190)
    val email: String,
    @Column(length = 120)
    var username: String? = null,
    @Column
    var userId: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: SignupVerificationStatus,
    @Column(nullable = false, length = 64)
    val requestedIp: String,
    @Column(length = 64)
    var confirmedIp: String? = null,
    @Column(nullable = false)
    val expiresAt: LocalDateTime,
    @Column(nullable = false)
    val createdAt: LocalDateTime,
    @Column
    var confirmedAt: LocalDateTime? = null,
    @Column(length = 500)
    var detail: String? = null,
) {
    fun isExpired(now: LocalDateTime): Boolean = now.isAfter(expiresAt)
}

enum class SignupVerificationType {
    EMAIL_SIGNUP_VERIFY,
    ACCOUNT_CONFIRM,
}

enum class SignupVerificationStatus {
    PENDING,
    VERIFIED,
    USED,
    REJECTED,
    EXPIRED,
}
