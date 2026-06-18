package com.sleekydz86.idolglow.admin.application.mapper

import com.sleekydz86.idolglow.admin.application.dto.AdminUserSummaryResult
import com.sleekydz86.idolglow.user.auth.domain.UserOAuth
import com.sleekydz86.idolglow.user.user.domain.User

object AdminUserResultMapper {
    fun toSummary(
        user: User,
        oauths: List<UserOAuth> = emptyList(),
    ): AdminUserSummaryResult =
        AdminUserSummaryResult(
            id = user.id,
            email = user.email,
            nickname = user.nickname.value,
            role = user.role,
            accountStatus = user.accountStatus,
            loginFailCount = user.loginFailCount,
            locked = user.isPlatformLocked(),
            platformUsername = user.platformUsername,
            profileImageUrl = resolveProfileImageUrl(user, oauths),
            lastLoginAt = user.lastLoginAt,
            oauthLinked = oauths.isNotEmpty(),
            oauthProviders = oauths.map { it.provider.name }.distinct(),
        )

    private fun resolveProfileImageUrl(
        user: User,
        oauths: List<UserOAuth>,
    ): String? {
        val primary = user.profileImageUrl?.trim()?.takeIf { it.isNotEmpty() }
        if (primary != null) {
            return primary
        }

        return oauths
            .sortedByDescending { it.provider.name == "GOOGLE" }
            .firstNotNullOfOrNull { oauth -> oauth.profileImageUrl?.trim()?.takeIf { it.isNotEmpty() } }
    }
}
