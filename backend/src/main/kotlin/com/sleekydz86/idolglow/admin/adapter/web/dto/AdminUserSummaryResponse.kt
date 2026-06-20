package com.sleekydz86.idolglow.admin.adapter.web.dto

data class AdminUserSummaryResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val role: String,
    val roleLabel: String,
    val accountStatus: String,
    val accountStatusLabel: String,
    val loginFailCount: Int,
    val locked: Boolean,
    val platformUsername: String?,
    val profileImageUrl: String?,
    val lastLoginAt: String?,
    val oauthLinked: Boolean,
    val oauthProviders: List<String>,
)
