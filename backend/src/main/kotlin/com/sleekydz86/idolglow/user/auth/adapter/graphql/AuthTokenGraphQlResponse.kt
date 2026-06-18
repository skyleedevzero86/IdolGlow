package com.sleekydz86.idolglow.user.auth.graphql

import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse

data class AuthTokenGraphQlResponse(
    val grantType: String,
    val accessToken: String,
    val accessTokenExpiresIn: String,
    val refreshToken: String,
    val refreshTokenExpiresIn: String,
) {
    companion object {
        fun from(response: TokenResponse): AuthTokenGraphQlResponse =
            AuthTokenGraphQlResponse(
                grantType = response.grantType,
                accessToken = response.accessToken,
                accessTokenExpiresIn = response.accessTokenExpiresIn.toString(),
                refreshToken = response.refreshToken,
                refreshTokenExpiresIn = response.refreshTokenExpiresIn.toString(),
            )
    }
}
