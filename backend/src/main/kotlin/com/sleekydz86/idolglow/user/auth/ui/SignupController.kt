package com.sleekydz86.idolglow.user.auth.ui

import com.sleekydz86.idolglow.user.auth.application.SignupService
import com.sleekydz86.idolglow.user.auth.application.dto.SignupCheckResponse
import com.sleekydz86.idolglow.user.auth.domain.dto.AccessTokenResponse
import com.sleekydz86.idolglow.user.auth.infrastructure.support.RefreshTokenCookieSupporter
import com.sleekydz86.idolglow.user.auth.ui.request.SignupRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth/signup")
class SignupController(
    private val signupService: SignupService,
    private val refreshTokenCookieSupporter: RefreshTokenCookieSupporter,
) {

    @GetMapping("/check-email")
    fun checkEmail(@RequestParam(required = false) email: String?): ResponseEntity<SignupCheckResponse> {
        val result = signupService.checkEmailField(email.orEmpty())
        return ResponseEntity.ok(SignupCheckResponse(result.available, result.code))
    }

    @GetMapping("/check-nickname")
    fun checkNickname(@RequestParam(required = false) nickname: String?): ResponseEntity<SignupCheckResponse> {
        val result = signupService.checkNicknameField(nickname.orEmpty())
        return ResponseEntity.ok(SignupCheckResponse(result.available, result.code))
    }

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
