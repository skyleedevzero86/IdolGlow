package com.sleekydz86.idolglow.platform.auth.ui

import com.sleekydz86.idolglow.admin.authverification.application.AuthVerificationAuditService
import com.sleekydz86.idolglow.platform.auth.http.ApiResponse
import com.sleekydz86.idolglow.platform.auth.dto.AccountRecoveryRequest
import com.sleekydz86.idolglow.platform.auth.dto.AccountRecoveryResponse
import com.sleekydz86.idolglow.platform.auth.dto.LoginRequest
import com.sleekydz86.idolglow.platform.auth.dto.PasswordResetRequest
import com.sleekydz86.idolglow.platform.auth.dto.RefreshTokenRequest
import com.sleekydz86.idolglow.platform.auth.dto.UserRegistrationRequest
import com.sleekydz86.idolglow.platform.auth.domain.JwtToken
import com.sleekydz86.idolglow.platform.auth.service.AccountRecoveryService
import com.sleekydz86.idolglow.platform.auth.service.AuthenticationService
import com.sleekydz86.idolglow.platform.user.domain.PlatformUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "플랫폼 인증", description = "이메일·JWT·계정 복구 API")
@RestController
@RequestMapping("/platform/auth")
@ConditionalOnProperty(prefix = "platform.auth", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class PlatformAuthController(
    private val authenticationService: AuthenticationService,
    private val accountRecoveryService: AccountRecoveryService,
    private val authVerificationAuditService: AuthVerificationAuditService,
) {

    @Operation(summary = "로그인", description = "비밀번호 정책 검증 후 액세스·리프레시 JWT 발급")
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<JwtToken>> =
        ResponseEntity.ok(ApiResponse.success(authenticationService.login(request)))

    @Operation(summary = "토큰 갱신")
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<ApiResponse<JwtToken>> =
        ResponseEntity.ok(ApiResponse.success(authenticationService.refresh(request)))

    @Operation(summary = "회원가입", description = "가입 즉시 사용 가능(계정 상태 승인)")
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: UserRegistrationRequest): ResponseEntity<ApiResponse<PlatformUser>> =
        ResponseEntity.ok(ApiResponse.success(authenticationService.register(request)))

    @Operation(summary = "계정 복구 시작", description = "이메일·사용자명 검증 후 복구 JWT 발급")
    @PostMapping("/recovery/initiate")
    fun initiateRecovery(
        @Valid @RequestBody request: AccountRecoveryRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<ApiResponse<AccountRecoveryResponse>> =
        try {
            val response = accountRecoveryService.initiate(request)
            authVerificationAuditService.log(
                verificationType = AuthVerificationAuditService.TYPE_ACCOUNT_RECOVERY_INITIATE,
                email = request.email,
                username = request.username,
                ipAddress = resolveClientIp(httpRequest),
                success = true,
                detail = "recovery initiated",
            )
            ResponseEntity.ok(ApiResponse.success(response))
        } catch (ex: Exception) {
            authVerificationAuditService.log(
                verificationType = AuthVerificationAuditService.TYPE_ACCOUNT_RECOVERY_INITIATE,
                email = request.email,
                username = request.username,
                ipAddress = resolveClientIp(httpRequest),
                success = false,
                detail = ex.message ?: ex.javaClass.simpleName,
            )
            throw ex
        }

    @Operation(summary = "비밀번호 재설정", description = "복구 토큰·JTI 소비 후 비밀번호 변경")
    @PostMapping("/recovery/reset")
    fun resetPassword(
        @Valid @RequestBody request: PasswordResetRequest,
    ): ResponseEntity<ApiResponse<AccountRecoveryResponse>> =
        ResponseEntity.ok(ApiResponse.success(accountRecoveryService.resetPassword(request)))

    private fun resolveClientIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
        if (!forwarded.isNullOrBlank()) return forwarded
        return request.remoteAddr ?: "unknown"
    }
}
