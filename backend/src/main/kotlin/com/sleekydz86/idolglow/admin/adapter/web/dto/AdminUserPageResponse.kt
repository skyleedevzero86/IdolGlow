package com.sleekydz86.idolglow.admin.ui.dto

import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserAccountStatus
import com.sleekydz86.idolglow.user.auth.domain.UserOAuth
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import java.time.format.DateTimeFormatter

data class AdminUserPageResponse(
    val users: List<AdminUserSummaryResponse>,
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
