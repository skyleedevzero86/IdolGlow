package com.sleekydz86.idolglow.user.user.application.dto

import com.sleekydz86.idolglow.user.auth.domain.UserOAuth
import com.sleekydz86.idolglow.user.user.domain.User
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class GetUserLoginInfoResponse(
    @Schema(description = "유저 ID", example = "1")
    val id: Long,
    @Schema(description = "유저 이메일", example = "test@gmail.com")
    val email: String,
    @Schema(description = "서비스 내 닉네임", example = "IdolGlow")
    val nickname: String,
    @Schema(description = "구글 계정 이름", example = "Idol Glow")
    val name: String?,
    @Schema(description = "구글 프로필 이미지 URL", example = "https://lh3.googleusercontent.com/a/sample")
    val picture: String?,
    @Schema(description = "마지막 로그인 시각", example = "2025-12-19T16:48:23.105699")
    val lastLoginAt: LocalDateTime?,
) {
    companion object {
        fun from(user: User, oauthProfile: UserOAuth? = null): GetUserLoginInfoResponse =
            GetUserLoginInfoResponse(
                id = user.id,
                email = user.email,
                nickname = user.nickname.value,
                name = oauthProfile?.profileName,
                picture = user.profileImageUrl?.takeIf { it.isNotBlank() }
                    ?: oauthProfile?.profileImageUrl,
                lastLoginAt = user.lastLoginAt
            )
    }
}
