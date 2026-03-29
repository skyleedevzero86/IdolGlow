package com.sleekydz86.idolglow.productpackage.admin.infrastructure

import com.sleekydz86.idolglow.productpackage.admin.domain.AdminAuditLog
import org.springframework.data.jpa.repository.JpaRepository

interface AdminAuditLogRepository : JpaRepository<AdminAuditLog, Long> {
    fun findTop200ByOrderByCreatedAtDesc(): List<AdminAuditLog>
}
