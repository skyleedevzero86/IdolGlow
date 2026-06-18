package com.sleekydz86.idolglow.admin.authverification.ui.dto

data class AuthVerificationAuditLogPageResponse(
    val logs: List<AuthVerificationAuditLogResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
