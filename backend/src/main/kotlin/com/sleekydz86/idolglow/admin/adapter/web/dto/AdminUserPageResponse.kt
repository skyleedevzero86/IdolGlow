package com.sleekydz86.idolglow.admin.ui.dto

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
