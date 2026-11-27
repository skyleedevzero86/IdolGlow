package com.sleekydz86.idolglow.user.auth.adapter.oauth

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.auth.AuthExceptionType
import com.sleekydz86.idolglow.user.auth.application.LoginFacade
import com.sleekydz86.idolglow.user.auth.infrastructure.support.RefreshTokenCookieSupporter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.DefaultOAuth2User

@DisplayName("OAuth 로그인 성공 처리: 카카오 이메일 거절")
class OAuth2SuccessHandlerKakaoTest {
    private val cookies = mockk<RefreshTokenCookieSupporter>(relaxed = true)
    private val loginFacade = mockk<LoginFacade>()
    private val handler = OAuth2SuccessHandler(cookies, loginFacade, "http://localhost:3000/auth/callback")
    private val principal = DefaultOAuth2User(emptyList(), mapOf("id" to 77L), "id")
    private val token = OAuth2AuthenticationToken(principal, emptyList(), "kakao")

    @DisplayName("K10 KAKAO_EMAIL_REQUIRED 면 쿠키 없이 콜백 주소에 oauth_error=kakao_email 을 붙여 보낸다")
    @Test
    fun redirectsWithKakaoEmailError() {
        // given
        every { loginFacade.login(any(), any()) } throws CustomException(AuthExceptionType.KAKAO_EMAIL_REQUIRED)
        val response = MockHttpServletResponse()

        // when
        handler.onAuthenticationSuccess(MockHttpServletRequest(), response, token)

        // then
        assertEquals("http://localhost:3000/auth/callback?oauth_error=kakao_email", response.redirectedUrl)
        verify(exactly = 0) { cookies.addRefreshTokenCookie(any(), any()) }
    }

    @DisplayName("K11 다른 오류는 그대로 던진다")
    @Test
    fun otherErrorsPropagate() {
        // given
        every { loginFacade.login(any(), any()) } throws CustomException(AuthExceptionType.USER_NOT_FOUND)

        // when
        val error =
            assertThrows(CustomException::class.java) {
                handler.onAuthenticationSuccess(MockHttpServletRequest(), MockHttpServletResponse(), token)
            }

        // then
        assertEquals(AuthExceptionType.USER_NOT_FOUND, error.getExceptionType())
    }
}
