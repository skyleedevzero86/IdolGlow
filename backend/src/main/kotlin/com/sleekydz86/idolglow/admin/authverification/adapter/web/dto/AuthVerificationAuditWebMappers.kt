package com.sleekydz86.idolglow.admin.authverification.adapter.web.dto

import com.sleekydz86.idolglow.admin.authverification.application.dto.AuthVerificationAuditLogPageResult
import com.sleekydz86.idolglow.admin.authverification.application.dto.AuthVerificationAuditLogResult

fun AuthVerificationAuditLogPageResult.toWebResponse(): AuthVerificationAuditLogPageResponse =
    AuthVerificationAuditLogPageResponse(
        logs = logs.map { it.toWebResponse() },
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )

fun AuthVerificationAuditLogResult.toWebResponse(): AuthVerificationAuditLogResponse =
    AuthVerificationAuditLogResponse(
        id = id,
        verificationType = verificationType,
        email = email,
        username = username,
        ipAddress = ipAddress,
        success = success,
        detail = detail,
        createdAt = createdAt,
    )
