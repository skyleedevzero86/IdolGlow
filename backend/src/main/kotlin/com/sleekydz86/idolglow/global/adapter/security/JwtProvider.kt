package com.sleekydz86.idolglow.global.adapter.security

import com.sleekydz86.idolglow.platform.auth.config.PlatformAuthProperties
import com.sleekydz86.idolglow.user.auth.infrastructure.support.RefreshTokenCookieSupporter
import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.security.Key
import java.util.Date

@Component
class JwtProvider(
    private val properties: PlatformAuthProperties,
) {

    private val log = LoggerFactory.getLogger(JwtProvider::class.java)

    private val signingKey: Key by lazy { JwtSigningKeyFactory.create(properties.jwt.secret) }

    fun generateToken(userId: Long, role: UserRole): TokenResponse {
        val issuedAt = Date()
        val now = issuedAt.time
        val accessTokenExpiresIn = Date(now + properties.jwt.accessTokenTtl.toMillis())
        val refreshTokenExpiresIn = Date(now + properties.jwt.refreshTokenTtl.toMillis())

        val accessToken = Jwts.builder()
            .setSubject(userId.toString())
            .setIssuer(properties.jwt.issuer)
            .claim(AUTHORITIES_KEY, role.name)
            .claim(TOKEN_TYPE_KEY, JwtTokenType.ACCESS.name)
            .setIssuedAt(issuedAt)
            .setExpiration(accessTokenExpiresIn)
            .signWith(signingKey, SignatureAlgorithm.HS512)
            .compact()

        val refreshToken = Jwts.builder()
            .setSubject(userId.toString())
            .setIssuer(properties.jwt.issuer)
            .claim(AUTHORITIES_KEY, role.name)
            .claim(TOKEN_TYPE_KEY, JwtTokenType.REFRESH.name)
            .setIssuedAt(issuedAt)
            .setExpiration(refreshTokenExpiresIn)
            .signWith(signingKey, SignatureAlgorithm.HS512)
            .compact()

        if (properties.jwt.logToken) {
            log.debug("액세스 토큰: {}", accessToken)
            log.debug("리프레시 토큰: {}", refreshToken)
        }

        return TokenResponse(
            grantType = BEARER_TYPE,
            accessToken = accessToken,
            accessTokenExpiresIn = accessTokenExpiresIn.time,
            refreshToken = refreshToken,
            refreshTokenExpiresIn = refreshTokenExpiresIn.time,
        )
    }

    fun resolveToken(request: HttpServletRequest): String? =
        request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith("$BEARER_TYPE ") }
            ?.substring(BEARER_TYPE.length + 1)
            ?: request.cookies
                ?.firstOrNull { it.name == RefreshTokenCookieSupporter.ACCESS_TOKEN_COOKIE }
                ?.value

    fun validateToken(token: String): Boolean =
        try {
            parseSignedClaims(token)
            true
        } catch (_: JwtException) {
            false
        }

    fun validateAccessToken(token: String): Boolean = validateToken(token, JwtTokenType.ACCESS)

    fun validateRefreshToken(token: String): Boolean = validateToken(token, JwtTokenType.REFRESH)

    private fun validateToken(token: String, expectedType: JwtTokenType): Boolean =
        try {
            extractTokenType(parseSignedClaims(token).body) == expectedType
        } catch (_: JwtException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }

    fun findAuthentication(token: String): Authentication {
        val claims = parseClaims(token)
        val userId = claims.subject.toLong()
        val role = UserRole.valueOf(
            claims[AUTHORITIES_KEY]?.toString()
                ?: throw IllegalArgumentException("토큰에 권한(auth) 클레임이 없습니다.")
        )

        return UsernamePasswordAuthenticationToken(
            userId.toString(),
            "",
            listOf(SimpleGrantedAuthority("ROLE_${role.name}")),
        )
    }

    fun getSubjectAsUserId(token: String): Long =
        parseClaims(token).subject.toLong()

    fun getTokenType(token: String): JwtTokenType =
        extractTokenType(parseClaims(token))

    private fun parseClaims(token: String): Claims =
        try {
            parseSignedClaims(token).body
        } catch (e: ExpiredJwtException) {
            e.claims
        }

    private fun parseSignedClaims(token: String): Jws<Claims> =
        Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)

    private fun extractTokenType(claims: Claims): JwtTokenType =
        JwtTokenType.valueOf(
            claims[TOKEN_TYPE_KEY]?.toString()
                ?: throw IllegalArgumentException("토큰에 타입(type) 클레임이 없습니다.")
        )

    companion object {
        private const val AUTHORITIES_KEY = "auth"
        private const val TOKEN_TYPE_KEY = "type"
        private const val BEARER_TYPE = "Bearer"
    }
}
