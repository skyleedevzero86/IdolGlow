package com.sleekydz86.idolglow.global.security

import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.security.Key
import java.util.Date

@Component
class JwtProvider(
    @Value("\${jwt.secret}")
    private val secretKey: String,
    @Value("\${jwt.log-token:true}")
    private val logToken: Boolean
) {

    private val log = LoggerFactory.getLogger(JwtProvider::class.java)

    private val key: Key by lazy {
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))
    }

    fun generateToken(userId: Long, role: UserRole): TokenResponse {
        val issuedAt = Date()
        val now = issuedAt.time
        val accessTokenExpiresIn = Date(now + ACCESS_TOKEN_EXPIRE_TIME)
        val refreshTokenExpiresIn = Date(now + REFRESH_TOKEN_EXPIRE_TIME)

        val accessToken = Jwts.builder()
            .setSubject(userId.toString())
            .claim(AUTHORITIES_KEY, role.name)
            .claim(TOKEN_TYPE_KEY, JwtTokenType.ACCESS.name)
            .setIssuedAt(issuedAt)
            .setExpiration(accessTokenExpiresIn)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()

        val refreshToken = Jwts.builder()
            .setSubject(userId.toString())
            .claim(AUTHORITIES_KEY, role.name)
            .claim(TOKEN_TYPE_KEY, JwtTokenType.REFRESH.name)
            .setIssuedAt(issuedAt)
            .setExpiration(refreshTokenExpiresIn)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()

        if (logToken) {
            log.debug("accessToken: $accessToken")
            log.debug("refreshToken: $refreshToken")
        }

        return TokenResponse(
            grantType = BEARER_TYPE,
            accessToken = accessToken,
            accessTokenExpiresIn = accessTokenExpiresIn.time,
            refreshToken = refreshToken,
            refreshTokenExpiresIn = refreshTokenExpiresIn.time
        )
    }

    fun resolveToken(request: HttpServletRequest): String? =
        request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith("$BEARER_TYPE ") }
            ?.substring(BEARER_TYPE.length + 1)

    fun validateToken(token: String): Boolean {
        return try {
            parseSignedClaims(token)
            true
        } catch (_: JwtException) {
            false
        }
    }

    fun validateAccessToken(token: String): Boolean = validateToken(token, JwtTokenType.ACCESS)

    fun validateRefreshToken(token: String): Boolean = validateToken(token, JwtTokenType.REFRESH)

    private fun validateToken(token: String, expectedType: JwtTokenType): Boolean {
        return try {
            extractTokenType(parseSignedClaims(token).body) == expectedType
        } catch (_: JwtException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    fun findAuthentication(token: String): Authentication {
        val claims = parseClaims(token)
        val userId = claims.subject.toLong()
        val role = UserRole.valueOf(
            claims[AUTHORITIES_KEY]?.toString()
                ?: throw IllegalArgumentException("Missing authority claim.")
        )
        return UsernamePasswordAuthenticationToken(
            userId.toString(),
            "",
            listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
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
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)

    private fun extractTokenType(claims: Claims): JwtTokenType =
        JwtTokenType.valueOf(
            claims[TOKEN_TYPE_KEY]?.toString()
                ?: throw IllegalArgumentException("Missing token type.")
        )

    companion object {
        private const val AUTHORITIES_KEY = "auth"
        private const val TOKEN_TYPE_KEY = "type"
        private const val BEARER_TYPE = "Bearer"
        private const val ACCESS_TOKEN_EXPIRE_TIME: Long = 1000L * 60 * 30
        private const val REFRESH_TOKEN_EXPIRE_TIME: Long = 1000L * 60 * 60 * 24 * 14
    }
}
