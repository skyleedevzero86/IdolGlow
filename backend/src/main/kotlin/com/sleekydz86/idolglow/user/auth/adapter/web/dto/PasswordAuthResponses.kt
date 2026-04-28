package com.sleekydz86.idolglow.user.auth.ui.dto

import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse

data class PasswordLoginResponse(
    val grantType: String,
    val accessToken: String,
    val accessTokenExpiresIn: Long,
    val requirePasswordChange: Boolean,
) {
    companion object {
        fun from(token: TokenResponse, requirePasswordChange: Boolean): PasswordLoginResponse =
            PasswordLoginResponse(
                grantType = token.grantType,
                accessToken = token.accessToken,
                accessTokenExpiresIn = token.accessTokenExpiresIn,
                requirePasswordChange = requirePasswordChange,
            )
    }
}

data class TemporaryPasswordResponse(
    val sent: Boolean,
    val message: String,
)
