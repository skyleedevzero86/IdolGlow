package com.sleekydz86.idolglow.user.user.application.dto

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
    @Schema(description = "연동된 OAuth 계정의 profile_name (user_oauths, 우선 Google 행)", example = "Idol Glow")
    val name: String?,
    @Schema(description = "프로필 이미지 URL (사용자 지정 또는 OAuth)")
    val picture: String?,
    @Schema(description = "OAuth 연동 여부 (user_oauths 존재)")
    val oauthLinked: Boolean,
    @Schema(description = "회원 역할", example = "USER")
    val role: String,
    @Schema(description = "마지막 로그인 시각", example = "2025-12-19T16:48:23.105699")
    val lastLoginAt: LocalDateTime?,
) {
    companion object {
        fun from(
            user: User,
            oauthLinked: Boolean,
            oauthProfileName: String?,
            profileImageFallback: String?,
        ): GetUserLoginInfoResponse =
            GetUserLoginInfoResponse(
                id = user.id,
                email = user.email,
                nickname = user.nickname.value,
                name = oauthProfileName,
                picture = user.profileImageUrl?.takeIf { it.isNotBlank() } ?: profileImageFallback,
                oauthLinked = oauthLinked,
                role = user.role.name,
                lastLoginAt = user.lastLoginAt
            )
    }
}
