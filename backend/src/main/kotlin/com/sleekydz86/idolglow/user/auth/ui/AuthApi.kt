package com.sleekydz86.idolglow.user.auth.ui

import com.nimbusds.oauth2.sdk.AccessTokenResponse
import com.sleekydz86.idolglow.user.auth.infrastructure.support.RefreshTokenCookieSupporter
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity

@Tag(name = "Auth", description = "인증/인가 관련 API")
interface AuthApi {

    @Operation(
        summary = "소셜 로그인",
        description = "provider에 해당하는 OAuth2 인증 플로우를 시작합니다. (/oauth2/authorization/{provider}로 리다이렉트)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "302", description = "OAuth2 Authorization endpoint로 리다이렉트"),
            ApiResponse(
                responseCode = "400",
                description = "지원하지 않는 provider",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun login(
        response: HttpServletResponse,
        @Parameter(description = "OAuth2 provider", example = "google")
        provider: String
    )

    @Operation(
        summary = "AccessToken 재발급",
        description = "refreshToken 쿠키와 CSRF 헤더를 이용해 AccessToken을 재발급합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "재발급 성공"),
            ApiResponse(responseCode = "401", description = "refreshToken 유효하지 않음"),
            ApiResponse(responseCode = "403", description = "CSRF 검증 실패")
        ]
    )
    fun reissue(
        @Parameter(`in` = ParameterIn.COOKIE, name = RefreshTokenCookieSupporter.REFRESH_TOKEN_COOKIE, required = true)
        refreshToken: String?,
        @Parameter(`in` = ParameterIn.COOKIE, name = RefreshTokenCookieSupporter.REFRESH_CSRF_COOKIE, required = true)
        refreshCsrfToken: String?,
        @Parameter(`in` = ParameterIn.HEADER, name = RefreshTokenCookieSupporter.REFRESH_CSRF_HEADER, required = true)
        refreshCsrfHeader: String?,
        response: HttpServletResponse
    ): ResponseEntity<AccessTokenResponse>

    @Operation(
        summary = "로그아웃",
        description = "refreshToken 쿠키와 CSRF 쿠키를 제거합니다."
    )
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    fun logout(
        @Parameter(`in` = ParameterIn.COOKIE, name = RefreshTokenCookieSupporter.REFRESH_CSRF_COOKIE, required = true)
        refreshCsrfToken: String?,
        @Parameter(`in` = ParameterIn.HEADER, name = RefreshTokenCookieSupporter.REFRESH_CSRF_HEADER, required = true)
        refreshCsrfHeader: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void>
}
