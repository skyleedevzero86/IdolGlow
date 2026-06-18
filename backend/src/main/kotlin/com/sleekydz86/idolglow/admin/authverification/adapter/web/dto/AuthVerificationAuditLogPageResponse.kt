package com.sleekydz86.idolglow.admin.authverification.adapter.web.dto

data class AuthVerificationAuditLogPageResponse(
    val logs: List<AuthVerificationAuditLogResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
