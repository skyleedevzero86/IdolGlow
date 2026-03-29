package com.sleekydz86.idolglow.user.auth.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

data class TestTokenResponse (
    @field:Schema(
        description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    val accessToken: String,

    @field:Schema(
        description = "리프레시 토큰", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIyI...")
    val refreshToken: String
)