package com.sleekydz86.idolglow.platform.auth.util

import com.sleekydz86.idolglow.global.adapter.security.JwtSigningKeyFactory
import com.sleekydz86.idolglow.platform.auth.config.PlatformAuthProperties
import com.sleekydz86.idolglow.platform.auth.application.dto.AuthenticatedUser
import com.sleekydz86.idolglow.platform.user.domain.PlatformUserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.security.Key
import java.time.Duration
import java.util.Date

@Component
@ConditionalOnProperty(prefix = "platform.auth", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class JwtTokenUtil(
    private val properties: PlatformAuthProperties,
) {

    private val log = LoggerFactory.getLogger(JwtTokenUtil::class.java)

    private val signingKey: Key by lazy { JwtSigningKeyFactory.create(properties.jwt.secret) }

    private fun accessTokenExpirationMillis(): Long = properties.jwt.accessTokenTtl.toMillis()
    private fun refreshTokenExpirationMillis(): Long = properties.jwt.refreshTokenTtl.toMillis()

    fun generateAccessToken(email: String, role: PlatformUserRole): String =
        generateToken(email, "ACCESS", role, accessTokenExpirationMillis())

    fun generateRefreshToken(email: String): String =
        generateToken(email, "REFRESH", null, refreshTokenExpirationMillis())

    private fun generateToken(
        email: String,
        tokenType: String,
        role: PlatformUserRole?,
        expirationMillis: Long,
    ): String {
        val claims = HashMap<String, Any>()
        claims["tokenType"] = tokenType
        if (role != null) {
            claims["role"] = role.name
        }
        val now = Date()
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(email)
            .setIssuer(properties.jwt.issuer)
            .setIssuedAt(now)
            .setExpiration(Date(now.time + expirationMillis))
            .signWith(signingKey, SignatureAlgorithm.HS512)
            .compact()
    }

    fun validateToken(token: String): Boolean =
        try {
            parseSignedClaims(token)
            true
        } catch (_: JwtException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }

    fun isTokenExpired(token: String): Boolean =
        try {
            val claims = parseSignedClaims(token)
            claims.expiration.before(Date())
        } catch (_: JwtException) {
            true
        } catch (_: IllegalArgumentException) {
            true
        }

    fun isTokenExpired(claims: Claims): Boolean = claims.expiration.before(Date())

    fun getEmailFromToken(token: String): String? =
        try {
            parseSignedClaims(token).subject
        } catch (_: JwtException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    fun getRoleFromToken(token: String): PlatformUserRole =
        try {
            val claims = parseSignedClaims(token)
            when (val roleStr = claims["role"] as? String) {
                "ADMIN", "MANAGER" -> PlatformUserRole.ADMIN
                "USER", null -> PlatformUserRole.USER
                else -> PlatformUserRole.USER
            }
        } catch (_: JwtException) {
            PlatformUserRole.USER
        } catch (_: IllegalArgumentException) {
            PlatformUserRole.USER
        }

    private fun parseSignedClaims(token: String): Claims =
        Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .body

    fun getAccessTokenExpirationMillis(): Long = accessTokenExpirationMillis()

    fun validateRefreshToken(token: String): Boolean =
        try {
            val claims = parseSignedClaims(token)
            "REFRESH" == claims["tokenType"] as? String
        } catch (_: JwtException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }

    fun getEmailFromRefreshToken(token: String): String? = getEmailFromToken(token)

    fun getTokenType(token: String): String? =
        try {
            parseSignedClaims(token)["tokenType"] as? String
        } catch (_: JwtException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    fun createAuthenticatedUser(token: String): AuthenticatedUser? =
        try {
            val email = getEmailFromToken(token) ?: return null
            val role = getRoleFromToken(token)
            AuthenticatedUser.builder()
                .email(email)
                .role(role.name)
                .build()
        } catch (e: Exception) {
            log.warn("토큰으로 인증 사용자 정보를 만들지 못했습니다: {}", e.message)
            null
        }

    fun generateRecoveryToken(email: String, username: String, jti: String): String {
        val claims = HashMap<String, Any>()
        claims["email"] = email
        claims["username"] = username
        claims["type"] = "recovery"
        claims["jti"] = jti
        val ttl: Duration = properties.jwt.recoveryTokenTtl
        return createToken("$email:$username", claims, ttl)
    }

    private fun createToken(subject: String, claims: Map<String, Any>, expiration: Duration): String {
        val now = Date()
        val expirationDate = Date(now.time + expiration.toMillis())
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuer(properties.jwt.issuer)
            .setIssuedAt(now)
            .setExpiration(expirationDate)
            .signWith(signingKey, SignatureAlgorithm.HS512)
            .compact()
    }

    fun getUserInfoFromRecoveryToken(token: String): String {
        val claims = parseSignedClaims(token)
        val type = claims["type"] as? String
        if (type != "recovery") {
            throw IllegalArgumentException("유효하지 않은 복구 토큰입니다.")
        }
        val email = claims["email"] as? String ?: throw IllegalArgumentException("유효하지 않은 복구 토큰입니다.")
        val username = claims["username"] as? String ?: throw IllegalArgumentException("유효하지 않은 복구 토큰입니다.")
        return "$email:$username"
    }

    fun getJtiFromRecoveryToken(token: String): String? {
        return try {
            val claims = parseSignedClaims(token)
            if (claims["type"] as? String != "recovery") {
                null
            } else {
                claims["jti"] as? String
            }
        } catch (_: Exception) {
            null
        }
    }

    fun validateRecoveryToken(token: String): Boolean =
        try {
            val claims = parseSignedClaims(token)
            val type = claims["type"] as? String
            val jti = claims["jti"] as? String
            type == "recovery" && !jti.isNullOrBlank() && !isTokenExpired(claims)
        } catch (_: Exception) {
            false
        }

    fun generateApiToken(email: String, apiKey: String): String {
        val claims = HashMap<String, Any>()
        claims["type"] = "API"
        claims["apiKey"] = apiKey
        val now = Date()
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(email)
            .setIssuer(properties.jwt.issuer)
            .setIssuedAt(now)
            .signWith(signingKey, SignatureAlgorithm.HS512)
            .compact()
    }

    fun validateApiToken(token: String, expectedApiKey: String): Boolean =
        try {
            val claims = parseSignedClaims(token)
            val type = claims["type"] as? String
            val apiKey = claims["apiKey"] as? String
            type == "API" && expectedApiKey == apiKey
        } catch (_: Exception) {
            false
        }

    fun getExpirationDateFromToken(token: String): Date? =
        try {
            parseSignedClaims(token).expiration
        } catch (_: Exception) {
            null
        }

    fun getRemainingTime(token: String): Long =
        try {
            val expiration = getExpirationDateFromToken(token) ?: return 0
            val remainingTime = expiration.time - System.currentTimeMillis()
            if (remainingTime > 0) remainingTime else 0
        } catch (_: Exception) {
            0
        }
}
