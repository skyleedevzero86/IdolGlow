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
) {
    companion object {
        fun from(user: User, oauths: List<UserOAuth> = emptyList()): AdminUserSummaryResponse =
            AdminUserSummaryResponse(
                id = user.id,
                email = user.email,
                nickname = user.nickname.value,
                role = user.role.name,
                roleLabel = user.role.label(),
                accountStatus = user.accountStatus.name,
                accountStatusLabel = user.accountStatus.label(),
                loginFailCount = user.loginFailCount,
                locked = user.isPlatformLocked(),
                platformUsername = user.platformUsername,
                profileImageUrl = resolveProfileImageUrl(user, oauths),
                lastLoginAt = user.lastLoginAt?.format(adminUserDateTimeFormatter),
                oauthLinked = oauths.isNotEmpty(),
                oauthProviders = oauths.map { it.provider.name }.distinct(),
            )

        private fun resolveProfileImageUrl(user: User, oauths: List<UserOAuth>): String? {
            val primary = user.profileImageUrl?.trim()?.takeIf { it.isNotEmpty() }
            if (primary != null) {
                return primary
            }

            return oauths
                .sortedByDescending { it.provider.name == "GOOGLE" }
                .firstNotNullOfOrNull { oauth -> oauth.profileImageUrl?.trim()?.takeIf { it.isNotEmpty() } }
        }
    }
}

private val adminUserDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

private fun UserRole.label(): String =
    when (this) {
        UserRole.USER -> "일반회원"
        UserRole.ADMIN -> "관리자"
    }

private fun UserAccountStatus.label(): String =
    when (this) {
        UserAccountStatus.PENDING -> "대기"
        UserAccountStatus.APPROVED -> "승인"
        UserAccountStatus.REJECTED -> "거절"
        UserAccountStatus.SUSPENDED -> "정지"
        UserAccountStatus.WITHDRAWN -> "탈퇴"
    }
