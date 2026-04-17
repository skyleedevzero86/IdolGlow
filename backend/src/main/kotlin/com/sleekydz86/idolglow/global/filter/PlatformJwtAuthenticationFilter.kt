package com.sleekydz86.idolglow.platform.auth.filter

import com.sleekydz86.idolglow.platform.auth.config.PlatformAuthProperties
import com.sleekydz86.idolglow.platform.auth.util.JwtTokenUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

@Component
@ConditionalOnProperty(prefix = "platform.auth", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class PlatformJwtAuthenticationFilter(
    private val jwtTokenUtil: JwtTokenUtil,
    private val userDetailsService: UserDetailsService,
    private val properties: PlatformAuthProperties,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(PlatformJwtAuthenticationFilter::class.java)
    private val pathMatcher = AntPathMatcher()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestURI = request.requestURI
        try {
            val token = extractTokenFromRequest(request)
            if (StringUtils.hasText(token)) {
                val t = token!!
                if (jwtTokenUtil.validateToken(t)) {
                    val tokenType = jwtTokenUtil.getTokenType(t)
                    if ("ACCESS" == tokenType) {
                        val authentication = getAuthentication(t)
                        if (authentication != null && authentication.isAuthenticated) {
                            SecurityContextHolder.getContext().authentication = authentication
                            log.debug("인증 설정 완료: {}", authentication.name)
                        } else {
                            log.warn("Failed to build authentication from token")
                        }
                    }
                } else {
                    log.warn("JWT 검증 실패: {}", requestURI)
                }
            } else {
                log.debug("JWT 없음: {}", requestURI)
            }
        } catch (e: Exception) {
            log.error("JWT 처리 오류 {}: {}", requestURI, e.message, e)
        }
        filterChain.doFilter(request, response)
    }

    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7)
        }
        val cookieName = properties.http.accessTokenCookieName
        val cookies = request.cookies ?: return null
        val tokenCookie = cookies.find { it.name == cookieName } ?: return null
        val token = tokenCookie.value
        return if (StringUtils.hasText(token)) token else null
    }

    private fun getAuthentication(token: String) =
        try {
            val email = jwtTokenUtil.getEmailFromToken(token)
            if (email != null) {
                val userDetails = userDetailsService.loadUserByUsername(email)
                UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
            } else {
                null
            }
        } catch (e: Exception) {
            log.error("인증 객체 생성 실패: {}", e.message)
            null
        }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        val patterns = properties.http.jwtFilterPermitPaths
        for (pattern in patterns) {
            if (pathMatcher.match(pattern, path)) {
                return true
            }
        }
        return false
    }
}
