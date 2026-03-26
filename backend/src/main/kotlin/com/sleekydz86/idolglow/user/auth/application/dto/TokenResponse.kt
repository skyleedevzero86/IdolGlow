package com.sleekydz86.idolglow.user.auth.application.dto

data class TokenResponse(
    val grantType: String,
    val accessToken: String,
    val accessTokenExpiresIn: Long,
    val refreshToken: String,
    val refreshTokenExpiresIn: Long
) {}