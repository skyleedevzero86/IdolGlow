package com.sleekydz86.idolglow.admin.application.dto

import com.sleekydz86.idolglow.user.user.domain.UserAccountStatus
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import java.time.LocalDateTime

data class AdminUserPageResult(
    val users: List<AdminUserSummaryResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val totalUsers: Long,
    val adminCount: Long,
    val suspendedCount: Long,
    val withdrawnCount: Long,
)

data class AdminUserSummaryResult(
    val id: Long,
    val email: String,
    val nickname: String,
    val role: UserRole,
    val accountStatus: UserAccountStatus,
    val loginFailCount: Int,
    val locked: Boolean,
    val platformUsername: String?,
    val profileImageUrl: String?,
    val lastLoginAt: LocalDateTime?,
    val oauthLinked: Boolean,
    val oauthProviders: List<String>,
)
