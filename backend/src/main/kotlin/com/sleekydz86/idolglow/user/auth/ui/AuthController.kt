package com.sleekydz86.idolglow.user.auth.ui

import com.sleekydz86.idolglow.user.auth.application.AuthService
import com.sleekydz86.idolglow.user.auth.application.PasswordRecoveryService
import com.sleekydz86.idolglow.user.auth.domain.dto.AccessTokenResponse
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import com.sleekydz86.idolglow.user.auth.infrastructure.support.RefreshTokenCookieSupporter
import com.sleekydz86.idolglow.user.auth.ui.dto.PasswordLoginResponse
import com.sleekydz86.idolglow.user.auth.ui.dto.TemporaryPasswordResponse
import com.sleekydz86.idolglow.user.auth.ui.request.PasswordLoginRequest
import com.sleekydz86.idolglow.user.auth.ui.request.TemporaryPasswordRequest
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val passwordRecoveryService: PasswordRecoveryService,
    private val refreshTokenCookieSupporter: RefreshTokenCookieSupporter,
) : AuthApi {

    @GetMapping("/login/{provider}")
    override fun login(response: HttpServletResponse, @PathVariable("provider") provider: String) {
        if (!AuthProvider.isAllowedRegistrationId(provider)) {
            throw IllegalArgumentException("지원하지 않는 로그인 제공자입니다: $provider")
        }
        response.sendRedirect("/oauth2/authorization/$provider")
    }

    @PostMapping("/reissue")
    override fun reissue(
        @CookieValue(value = RefreshTokenCookieSupporter.REFRESH_TOKEN_COOKIE, required = false) refreshToken: String?,
        @CookieValue(value = RefreshTokenCookieSupporter.REFRESH_CSRF_COOKIE, required = false) refreshCsrfToken: String?,
        @RequestHeader(value = RefreshTokenCookieSupporter.REFRESH_CSRF_HEADER, required = false) refreshCsrfHeader: String?,
        response: HttpServletResponse
    ): ResponseEntity<AccessTokenResponse> {
        refreshTokenCookieSupporter.validateCsrf(refreshCsrfHeader, refreshCsrfToken)

        val tokenResponse = authService.reissue(refreshToken.orEmpty())
        refreshTokenCookieSupporter.addAuthenticationCookies(response, tokenResponse)

        return ResponseEntity.ok(AccessTokenResponse.from(tokenResponse))
    }

    @PostMapping("/logout")
    override fun logout(
        @CookieValue(value = RefreshTokenCookieSupporter.REFRESH_CSRF_COOKIE, required = false) refreshCsrfToken: String?,
        @RequestHeader(value = RefreshTokenCookieSupporter.REFRESH_CSRF_HEADER, required = false) refreshCsrfHeader: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        refreshTokenCookieSupporter.validateCsrf(refreshCsrfHeader, refreshCsrfToken)
        refreshTokenCookieSupporter.expireAuthenticationCookies(response)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/password/login")
    fun loginByPassword(
        @Valid @RequestBody request: PasswordLoginRequest,
        response: HttpServletResponse,
    ): ResponseEntity<PasswordLoginResponse> {
        val result = passwordRecoveryService.loginWithPassword(request.email, request.password)
        refreshTokenCookieSupporter.addAuthenticationCookies(response, result.token)
        return ResponseEntity.ok(PasswordLoginResponse.from(result.token, result.requirePasswordChange))
    }

    @PostMapping("/password/temporary")
    fun issueTemporaryPassword(
        @Valid @RequestBody request: TemporaryPasswordRequest,
        httpServletRequest: HttpServletRequest,
    ): ResponseEntity<TemporaryPasswordResponse> {
        val forwardedFor = httpServletRequest.getHeader("X-Forwarded-For")
        val ip = forwardedFor?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
            ?: httpServletRequest.remoteAddr.orEmpty().ifBlank { "unknown" }
        val sent = passwordRecoveryService.issueTemporaryPassword(request.email, ip)
        val message = if (sent) {
            "등록된 이메일로 임시 비밀번호를 발송했습니다."
        } else {
            "가입된 이메일을 찾을 수 없거나 비밀번호 로그인을 지원하지 않는 계정입니다."
        }
        return ResponseEntity.ok(TemporaryPasswordResponse(sent = sent, message = message))
    }

    @PostMapping("/account/find-id")
    fun findAccountId(
        @Valid @RequestBody request: TemporaryPasswordRequest,
        httpServletRequest: HttpServletRequest,
    ): ResponseEntity<TemporaryPasswordResponse> {
        val forwardedFor = httpServletRequest.getHeader("X-Forwarded-For")
        val ip = forwardedFor?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
            ?: httpServletRequest.remoteAddr.orEmpty().ifBlank { "unknown" }
        val sent = passwordRecoveryService.sendAccountIdReminder(request.email, ip)
        val message = if (sent) {
            "등록된 이메일로 아이디 안내 메일을 발송했습니다."
        } else {
            "가입된 이메일을 찾을 수 없습니다."
        }
        return ResponseEntity.ok(TemporaryPasswordResponse(sent = sent, message = message))
    }
}
