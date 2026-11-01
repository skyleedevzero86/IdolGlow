package com.sleekydz86.idolglow.user.auth.adapter.web

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.auth.AuthExceptionType
import com.sleekydz86.idolglow.user.auth.application.AuthService
import com.sleekydz86.idolglow.user.auth.application.PasswordRecoveryService
import com.sleekydz86.idolglow.user.auth.infrastructure.support.RefreshTokenCookieSupporter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletResponse

@DisplayName("리프레시 재발급 쿠키")
class AuthControllerReissueCookieTest {
    private val authService = mockk<AuthService>()
    private val refreshTokenCookieSupporter = mockk<RefreshTokenCookieSupporter>(relaxed = true)
    private val controller =
        AuthController(
            authService,
            mockk<PasswordRecoveryService>(relaxed = true),
            refreshTokenCookieSupporter,
        )

    @Test
    fun `유효하지 않은 리프레시 토큰이면 인증 쿠키를 만료한다`() {
        // given
        every { authService.reissue(any()) } throws CustomException(AuthExceptionType.INVALID_REFRESH_TOKEN)
        val response = MockHttpServletResponse()

        // when
        assertThrows(CustomException::class.java) {
            controller.reissue("stale", "csrf", "csrf", response)
        }

        // then
        verify(exactly = 1) { refreshTokenCookieSupporter.expireAuthenticationCookies(response) }
    }

    @Test
    fun `다른 인증 예외에서는 인증 쿠키를 지우지 않는다`() {
        // given
        every { authService.reissue(any()) } throws CustomException(AuthExceptionType.USER_NOT_FOUND)
        val response = MockHttpServletResponse()

        // when
        assertThrows(CustomException::class.java) {
            controller.reissue("stale", "csrf", "csrf", response)
        }

        // then
        verify(exactly = 0) { refreshTokenCookieSupporter.expireAuthenticationCookies(any()) }
    }
}
