package com.sleekydz86.idolglow.productpackage.admin.application.dto

import com.sleekydz86.idolglow.productpackage.admin.domain.AdminAuditLog
import io.swagger.v3.oas.annotations.media.Schema

data class AdminAuditLogResponse(
    @field:Schema(description = "로그 ID")
    val id: Long,
    @field:Schema(description = "관리자 사용자 ID")
    val adminUserId: Long,
    @field:Schema(description = "액션 코드")
    val actionCode: String,
    @field:Schema(description = "대상 유형")
    val targetType: String,
    @field:Schema(description = "대상 ID")
    val targetId: Long?,
    @field:Schema(description = "상세")
    val detail: String?,
    @field:Schema(description = "기록 시각")
    val createdAt: String,
) {
    companion object {
        fun from(entity: AdminAuditLog): AdminAuditLogResponse =
            AdminAuditLogResponse(
                id = entity.id,
                adminUserId = entity.adminUserId,
                actionCode = entity.actionCode,
                targetType = entity.targetType,
                targetId = entity.targetId,
                detail = entity.detail,
                createdAt = entity.createdAt.toString(),
            )
    }
}
