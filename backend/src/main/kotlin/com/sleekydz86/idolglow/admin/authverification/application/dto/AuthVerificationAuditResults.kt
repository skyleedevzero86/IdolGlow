package com.sleekydz86.idolglow.admin.authverification.application.dto

import com.sleekydz86.idolglow.admin.authverification.domain.AuthVerificationAuditLog
import java.time.LocalDateTime

data class AuthVerificationAuditLogPageResult(
    val logs: List<AuthVerificationAuditLogResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class AuthVerificationAuditLogResult(
    val id: Long,
    val verificationType: String,
    val email: String?,
    val username: String?,
    val ipAddress: String,
    val success: Boolean,
    val detail: String?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(log: AuthVerificationAuditLog): AuthVerificationAuditLogResult =
            AuthVerificationAuditLogResult(
                id = requireNotNull(log.id),
                verificationType = log.verificationType,
                email = log.email,
                username = log.username,
                ipAddress = log.ipAddress,
                success = log.success,
                detail = log.detail,
                createdAt = log.createdAt,
            )
    }
}
