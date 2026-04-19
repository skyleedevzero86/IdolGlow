package com.sleekydz86.idolglow.platform.auth.domain

import java.time.LocalDateTime
import java.util.Objects

class JwtToken private constructor(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: LocalDateTime,
    val tokenType: String,
) {

    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val jwtToken = other as JwtToken
        return accessToken == jwtToken.accessToken
    }

    override fun hashCode(): Int = Objects.hash(accessToken)

    companion object {
        @JvmStatic
        fun builder(): JwtTokenBuilder = JwtTokenBuilder()
    }

    class JwtTokenBuilder {
        private var accessToken: String? = null
        private var refreshToken: String? = null
        private var expiresAt: LocalDateTime? = null
        private var tokenType: String = "Bearer"

        fun accessToken(accessToken: String) = apply { this.accessToken = accessToken }
        fun refreshToken(refreshToken: String) = apply { this.refreshToken = refreshToken }
        fun expiresIn(expiresAt: LocalDateTime) = apply { this.expiresAt = expiresAt }
        fun tokenType(tokenType: String) = apply { this.tokenType = tokenType }

        fun build(): JwtToken = JwtToken(
            accessToken = accessToken ?: error("액세스 토큰이 필요합니다."),
            refreshToken = refreshToken ?: error("리프레시 토큰이 필요합니다."),
            expiresAt = expiresAt ?: error("만료 시각이 필요합니다."),
            tokenType = tokenType,
        )
    }
}
