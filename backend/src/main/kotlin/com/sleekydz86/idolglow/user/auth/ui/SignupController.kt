package com.sleekydz86.idolglow.user.auth.ui

import com.sleekydz86.idolglow.admin.authverification.application.AuthVerificationAuditService
import com.sleekydz86.idolglow.user.auth.application.SignupService
import com.sleekydz86.idolglow.user.auth.application.SignupVerificationService
import com.sleekydz86.idolglow.user.auth.application.dto.SignupCheckResponse
import com.sleekydz86.idolglow.user.auth.domain.dto.AccessTokenResponse
import com.sleekydz86.idolglow.user.auth.infrastructure.support.RefreshTokenCookieSupporter
import com.sleekydz86.idolglow.user.auth.ui.request.SignupRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "회원가입", description = "이메일·닉네임 중복 확인 및 회원가입(액세스 토큰 발급, 리프레시 토큰은 HttpOnly 쿠키).")
@RestController
@RequestMapping("/auth/signup")
class SignupController(
    private val signupService: SignupService,
    private val refreshTokenCookieSupporter: RefreshTokenCookieSupporter,
    private val authVerificationAuditService: AuthVerificationAuditService,
    private val signupVerificationService: SignupVerificationService,
) {

    @SecurityRequirements
    @Operation(
        summary = "이메일 사용 가능 여부 확인",
        description = "회원가입 폼에서 이메일 중복·형식을 검사합니다. `available=true`이면 사용 가능, 불가 시 `code`에 사유(BLANK, INVALID_FORMAT, TAKEN 등)가 올 수 있습니다.",
    )
    @ApiResponse(responseCode = "200", description = "검사 결과")
    @GetMapping("/check-email")
    fun checkEmail(
        @Parameter(description = "확인할 이메일 주소", example = "user@example.com")
        @RequestParam(required = false) email: String?,
        request: HttpServletRequest,
    ): ResponseEntity<SignupCheckResponse> {
        val result = signupService.checkEmailField(email.orEmpty())
        authVerificationAuditService.log(
            verificationType = AuthVerificationAuditService.TYPE_SIGNUP_EMAIL_CHECK,
            email = email,
            username = null,
            ipAddress = resolveClientIp(request),
            success = result.available,
            detail = result.code,
        )
        return ResponseEntity.ok(SignupCheckResponse(result.available, result.code))
    }

    @SecurityRequirements
    @Operation(
        summary = "닉네임 사용 가능 여부 확인",
        description = "닉네임 중복·규칙(형식)을 검사합니다. 응답 형식은 이메일 확인과 동일합니다.",
    )
    @ApiResponse(responseCode = "200", description = "검사 결과")
    @GetMapping("/check-nickname")
    fun checkNickname(
        @Parameter(description = "확인할 닉네임", example = "idol_fan")
        @RequestParam(required = false) nickname: String?,
    ): ResponseEntity<SignupCheckResponse> {
        val result = signupService.checkNicknameField(nickname.orEmpty())
        return ResponseEntity.ok(SignupCheckResponse(result.available, result.code))
    }

    @SecurityRequirements
    @Operation(summary = "이메일 인증 메일 발송", description = "회원가입 이메일 인증 링크를 발송합니다. 링크 유효시간은 5분입니다.")
    @PostMapping("/email-verification/request")
    fun requestEmailVerification(
        @RequestParam(required = false) email: String?,
        request: HttpServletRequest,
    ): ResponseEntity<Map<String, Any>> {
        val normalized = email?.trim().orEmpty()
        val result = signupService.checkEmailField(normalized)
        if (!result.available) {
            return ResponseEntity.ok(mapOf("sent" to false, "reason" to (result.code ?: "INVALID")))
        }
        signupVerificationService.requestSignupEmailVerification(
            email = normalized.lowercase(),
            ipAddress = resolveClientIp(request),
            callbackBaseUrl = resolvePublicBaseUrl(request),
        )
        return ResponseEntity.ok(mapOf("sent" to true, "expiresInSeconds" to 300))
    }

    @SecurityRequirements
    @Operation(summary = "이메일 인증 링크 확인", description = "메일로 받은 링크를 눌러 이메일 인증을 완료합니다.")
    @GetMapping("/email-verification/confirm")
    fun confirmEmailVerification(
        @RequestParam token: String,
        request: HttpServletRequest,
    ): ResponseEntity<String> {
        val confirmed = signupVerificationService.confirmSignupEmailVerification(token, resolveClientIp(request))
        return if (confirmed) {
            ResponseEntity.ok("이메일 인증이 완료되었습니다. 5분 이내 회원가입을 완료해 주세요.")
        } else {
            ResponseEntity.badRequest().body("인증 링크가 유효하지 않거나 만료되었습니다. 다시 인증해 주세요.")
        }
    }

    @SecurityRequirements
    @Operation(summary = "가입 후 계정 확인", description = "가입 확인 메일에서 '맞음/아님'을 선택합니다.")
    @GetMapping("/account-confirm")
    fun confirmSignedAccount(
        @RequestParam token: String,
        @RequestParam(defaultValue = "confirm") decision: String,
        request: HttpServletRequest,
    ): ResponseEntity<String> {
        val ok = signupVerificationService.confirmPostSignupAccount(
            tokenValue = token,
            decision = decision,
            ipAddress = resolveClientIp(request),
        )
        return if (ok) {
            ResponseEntity.ok(
                if (decision.equals("confirm", ignoreCase = true)) {
                    "계정 확인이 완료되었습니다. 정상적으로 로그인할 수 있습니다."
                } else {
                    "아님 선택이 접수되어 계정이 정지되었습니다. 관리자에게 문의해 주세요."
                },
            )
        } else {
            ResponseEntity.badRequest().body("확인 링크가 유효하지 않거나 만료되었습니다.")
        }
    }

    @SecurityRequirements
    @Operation(
        summary = "회원가입",
        description = "계정을 생성하고 액세스 토큰을 반환합니다. 리프레시 토큰은 응답 본문이 아닌 HttpOnly 쿠키로 설정됩니다.",
    )
    @ApiResponse(responseCode = "200", description = "가입 성공, 액세스 토큰")
    @PostMapping
    fun signup(
        @Valid @RequestBody request: SignupRequest,
        httpRequest: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<AccessTokenResponse> {
        val tokenResponse = signupService.signup(
            email = request.email,
            rawNickname = request.nickname,
            password = request.password,
            subscribeToUpdates = request.subscribeToUpdates,
        )
        refreshTokenCookieSupporter.addAuthenticationCookies(response, tokenResponse)
        val loginUser = signupService.loadByEmail(request.email.trim().lowercase()) ?: return ResponseEntity.ok(AccessTokenResponse.from(tokenResponse))
        signupVerificationService.sendPostSignupAccountConfirmMail(
            user = loginUser,
            ipAddress = resolveClientIp(httpRequest),
            callbackBaseUrl = resolvePublicBaseUrl(httpRequest),
        )
        return ResponseEntity.ok(AccessTokenResponse.from(tokenResponse))
    }

    private fun resolveClientIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
        if (!forwarded.isNullOrBlank()) return forwarded
        return request.remoteAddr ?: "unknown"
    }

    private fun resolvePublicBaseUrl(request: HttpServletRequest): String {
        val scheme = request.scheme ?: "http"
        val host = request.serverName ?: "localhost"
        val port = request.serverPort
        val withPort = if ((scheme == "http" && port == 80) || (scheme == "https" && port == 443)) {
            ""
        } else {
            ":$port"
        }
        return "$scheme://$host$withPort"
    }
}
