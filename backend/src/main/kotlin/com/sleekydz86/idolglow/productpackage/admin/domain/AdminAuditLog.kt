package com.sleekydz86.idolglow.productpackage.admin.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "admin_audit_logs")
class AdminAuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "admin_user_id", nullable = false)
    val adminUserId: Long,

    @Column(name = "action_code", nullable = false, length = 64)
    val actionCode: String,

    @Column(name = "target_type", nullable = false, length = 64)
    val targetType: String,

    @Column(name = "target_id")
    val targetId: Long? = null,

    @Column(length = 2000)
    val detail: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,
)
