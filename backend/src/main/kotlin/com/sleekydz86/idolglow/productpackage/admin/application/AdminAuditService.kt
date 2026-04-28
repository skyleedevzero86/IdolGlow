package com.sleekydz86.idolglow.productpackage.admin.application

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminAuditLogResponse
import com.sleekydz86.idolglow.productpackage.admin.domain.AdminAuditLog
import com.sleekydz86.idolglow.productpackage.admin.infrastructure.AdminAuditLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AdminAuditService(
    private val adminAuditLogRepository: AdminAuditLogRepository,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {

    @Transactional
    fun log(actionCode: String, targetType: String, targetId: Long?, detail: String?) {
        val adminUserId = authenticatedUserIdResolver.resolveRequired()
        adminAuditLogRepository.save(
            AdminAuditLog(
                adminUserId = adminUserId,
                actionCode = actionCode,
                targetType = targetType,
                targetId = targetId,
                detail = detail?.take(2000),
                createdAt = LocalDateTime.now(),
            )
        )
    }

    @Transactional(readOnly = true)
    fun findRecentLogs(): List<AdminAuditLogResponse> =
        adminAuditLogRepository.findTop200ByOrderByCreatedAtDesc().map(AdminAuditLogResponse::from)
}
