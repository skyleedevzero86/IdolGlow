package com.sleekydz86.idolglow.platform.auth.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@ConditionalOnProperty(prefix = "platform.auth", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class PlatformSecurityHeadersFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        setSecurityHeaders(request, response)
        filterChain.doFilter(request, response)
    }

    private fun setSecurityHeaders(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        response.setHeader("X-XSS-Protection", "1; mode=block")
        response.setHeader("X-Frame-Options", "DENY")
        response.setHeader("X-Content-Type-Options", "nosniff")
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin")
        response.setHeader(
            "Permissions-Policy",
            "geolocation=(), microphone=(), camera=(), payment=(), usb=(), magnetometer=(), gyroscope=()",
        )
        val path = request.requestURI.lowercase()
        val contentSecurityPolicy = if (path.startsWith("/graphiql")) {
            "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://esm.sh; " +
                "style-src 'self' 'unsafe-inline' https://esm.sh; " +
                "img-src 'self' data: https:; " +
                "font-src 'self' data: https://esm.sh; " +
                "connect-src 'self' https: ws: wss:; " +
                "worker-src 'self' blob: https://esm.sh; " +
                "frame-ancestors 'none';"
        } else {
            "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "font-src 'self' data:; " +
                "connect-src 'self' https:; " +
                "frame-ancestors 'none';"
        }
        response.setHeader("Content-Security-Policy", contentSecurityPolicy)
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0")
        response.setHeader("Pragma", "no-cache")
        response.setHeader("Expires", "0")
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.lowercase()
        return path.contains("/actuator/health") ||
            path.contains("/actuator/metrics") ||
            path.contains("/health")
    }
}
