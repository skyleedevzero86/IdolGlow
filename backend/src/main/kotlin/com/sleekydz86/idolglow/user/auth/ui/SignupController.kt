package com.sleekydz86.idolglow.user.auth.ui

import com.sleekydz86.idolglow.user.auth.application.SignupService
import com.sleekydz86.idolglow.user.auth.application.dto.SignupCheckResponse
import com.sleekydz86.idolglow.user.auth.domain.dto.AccessTokenResponse
import com.sleekydz86.idolglow.user.auth.infrastructure.support.RefreshTokenCookieSupporter
import com.sleekydz86.idolglow.user.auth.ui.request.SignupRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
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
) {

    @SecurityRequirements
    @Operation(
        summary = "이메일 가입 가능 여부 확인",
        description = "회원가입 폼에서 이메일 중복·형식을 검사합니다. `available=true`이면 사용 가능, 불가 시 `code`에 사유(BLANK, INVALID_FORMAT, TAKEN 등)가 올 수 있습니다.",
    )
    @ApiResponse(responseCode = "200", description = "검사 결과")
    @GetMapping("/check-email")
    fun checkEmail(
        @Parameter(description = "확인할 이메일 주소", example = "user@example.com")
        @RequestParam(required = false) email: String?,
    ): ResponseEntity<SignupCheckResponse> {
        val result = signupService.checkEmailField(email.orEmpty())
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
    @Operation(
        summary = "회원가입",
        description = "계정을 생성하고 액세스 토큰을 반환합니다. 리프레시 토큰은 응답 본문이 아니라 **HttpOnly 쿠키**로 설정될 수 있습니다.",
    )
    @ApiResponse(responseCode = "200", description = "가입 성공, 액세스 토큰")
    @PostMapping
    fun signup(
        @Valid @RequestBody request: SignupRequest,
        response: HttpServletResponse,
    ): ResponseEntity<AccessTokenResponse> {
        val tokenResponse = signupService.signup(
            email = request.email,
            rawNickname = request.nickname,
            password = request.password,
        )
        refreshTokenCookieSupporter.addRefreshTokenCookie(response, tokenResponse.refreshToken)
        return ResponseEntity.ok(AccessTokenResponse.from(tokenResponse))
    }
}
