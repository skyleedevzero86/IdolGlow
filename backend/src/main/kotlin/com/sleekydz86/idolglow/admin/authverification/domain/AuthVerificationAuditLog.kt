package com.sleekydz86.idolglow.admin.authverification.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "auth_verification_audit_logs")
class AuthVerificationAuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false, length = 64)
    val verificationType: String,
    @Column(nullable = true, length = 190)
    val email: String?,
    @Column(nullable = true, length = 120)
    val username: String?,
    @Column(nullable = false, length = 64)
    val ipAddress: String,
    @Column(nullable = false)
    val success: Boolean,
    @Column(nullable = true, length = 500)
    val detail: String?,
    @Column(nullable = false)
    val createdAt: LocalDateTime,
)
