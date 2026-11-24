package com.sleekydz86.idolglow.productpackage.admin.application

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminAuditLogResponse
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminAuditTopActionRow
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminAuditTopActionsResponse
import com.sleekydz86.idolglow.productpackage.admin.domain.AdminAuditLog
import com.sleekydz86.idolglow.productpackage.admin.infrastructure.AdminAuditLogRepository
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AdminAuditService(
    private val adminAuditLogRepository: AdminAuditLogRepository,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
    private val environment: Environment,
) {
    @Transactional
    fun log(
        actionCode: String,
        targetType: String,
        targetId: Long?,
        detail: String?,
    ) {
        val adminUserId = authenticatedUserIdResolver.resolveRequired()
        adminAuditLogRepository.save(
            AdminAuditLog(
                adminUserId = adminUserId,
                actionCode = actionCode,
                targetType = targetType,
                targetId = targetId,
                detail = detail?.take(2000),
                createdAt = LocalDateTime.now(),
            ),
        )
    }

    @Transactional(readOnly = true)
    fun findRecentLogs(): List<AdminAuditLogResponse> =
        adminAuditLogRepository.findTop200ByOrderByCreatedAtDesc().map(AdminAuditLogResponse::from)

    @Transactional(readOnly = true)
    fun topRepeatedActions(): AdminAuditTopActionsResponse {
        val ranked = adminAuditLogRepository.countByActionCode().take(topSize())
        val choice = AdminAuditAutomation.choose(ranked.map { it.actionCode })
        val local = environment.activeProfiles.any { it.equals(LOCAL_PROFILE, ignoreCase = true) }
        return AdminAuditTopActionsResponse(
            basis = AdminAuditAutomation.basis(local),
            actions = ranked.mapIndexed { index, row ->
                AdminAuditTopActionRow(rank = index + 1, actionCode = row.actionCode, count = row.actionCount)
            },
            automationActionCode = choice.actionCode,
            automationReason = choice.reason,
        )
    }

    private fun topSize(): Int = 2 + 2 + 1

    companion object {
        private const val LOCAL_PROFILE = "local"
    }
}
