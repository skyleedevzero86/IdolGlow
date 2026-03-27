package com.sleekydz86.idolglow.user.user.application.dto

import com.sleekydz86.idolglow.user.user.domain.User
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class GetUserLoginInfoResponse(
    @Schema(description = "유저 ID", example = "1")
    val id: Long,
    @Schema(description = "유저 이메일", example = "test@gmail.com")
    val email: String,
    @Schema(description = "유저 닉네임", example = "doki")
    val nickname: String,
    @Schema(description = "마지막 로그인 시각", example = "2025-12-19T16:48:23.105699")
    val lastLoginAt: LocalDateTime?
) {
    companion object {
        fun from(user: User): GetUserLoginInfoResponse =
            GetUserLoginInfoResponse(
                id = user.id,
                email = user.email,
                nickname = user.nickname.value,
                lastLoginAt = user.lastLoginAt
            )
    }
}
