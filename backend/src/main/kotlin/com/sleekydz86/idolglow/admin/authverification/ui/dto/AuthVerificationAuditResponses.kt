package com.sleekydz86.idolglow.admin.authverification.ui.dto

import com.sleekydz86.idolglow.admin.authverification.domain.AuthVerificationAuditLog
import java.time.LocalDateTime

data class AuthVerificationAuditLogResponse(
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
        fun from(log: AuthVerificationAuditLog) = AuthVerificationAuditLogResponse(
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

data class AuthVerificationAuditLogPageResponse(
    val logs: List<AuthVerificationAuditLogResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
