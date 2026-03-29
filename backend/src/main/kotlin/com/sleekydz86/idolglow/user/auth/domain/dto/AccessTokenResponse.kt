package com.sleekydz86.idolglow.user.auth.domain.dto

import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse
import io.swagger.v3.oas.annotations.media.Schema

data class AccessTokenResponse(
    @field:Schema(description = "토큰 타입", example = "Bearer")
    val grantType: String,

    @field:Schema(
        description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val accessToken: String,

    @field:Schema(description = "액세스 토큰 만료 시간", example = "1700000000000")
    val accessTokenExpiresIn: Long
) {
    companion object {
        fun from(
            tokenResponse: TokenResponse
        ) : AccessTokenResponse =
            AccessTokenResponse(
                grantType = tokenResponse.grantType,
                accessToken = tokenResponse.accessToken,
                accessTokenExpiresIn = tokenResponse.accessTokenExpiresIn
            )
    }
}
