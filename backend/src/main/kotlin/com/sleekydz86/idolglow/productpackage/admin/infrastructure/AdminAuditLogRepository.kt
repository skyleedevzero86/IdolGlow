package com.sleekydz86.idolglow.productpackage.admin.infrastructure

import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminAuditActionCount
import com.sleekydz86.idolglow.productpackage.admin.domain.AdminAuditLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AdminAuditLogRepository : JpaRepository<AdminAuditLog, Long> {
    fun findTop200ByOrderByCreatedAtDesc(): List<AdminAuditLog>

    @Query(
        """
        select new com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminAuditActionCount(
            log.actionCode,
            count(log)
        )
        from AdminAuditLog log
        group by log.actionCode
        order by count(log) desc
        """,
    )
    fun countByActionCode(): List<AdminAuditActionCount>
}
