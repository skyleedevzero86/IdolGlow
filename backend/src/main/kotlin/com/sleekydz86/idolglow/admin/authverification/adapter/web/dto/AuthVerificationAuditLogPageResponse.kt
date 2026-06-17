package com.sleekydz86.idolglow.admin.authverification.ui.dto

import com.sleekydz86.idolglow.admin.authverification.domain.AuthVerificationAuditLog
import java.time.LocalDateTime

data class AuthVerificationAuditLogPageResponse(
    val logs: List<AuthVerificationAuditLogResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
