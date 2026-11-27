package com.sleekydz86.idolglow.user.auth.application.strategy

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.auth.AuthExceptionType
import com.sleekydz86.idolglow.user.auth.application.AuthService
import com.sleekydz86.idolglow.user.auth.application.LoginRequest
import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("네이버 로그인 전략")
class NaverLoginStrategyTest {
    private val authService = mockk<AuthService>()
    private val strategy = NaverLoginStrategy(authService)
    private val token = mockk<TokenResponse>()

    @DisplayName("연결이 없고 이메일도 없으면 NAVER_EMAIL_REQUIRED 로 거절하고 회원을 만들지 않는다")
    @Test
    fun rejectsNewUserWithoutEmail() {
        // given
        every { authService.isLinked(AuthProvider.NAVER, "n-1") } returns false

        // when
        val error = assertThrows(CustomException::class.java) { strategy.login(request(email = null)) }

        // then
        assertEquals(AuthExceptionType.NAVER_EMAIL_REQUIRED, error.getExceptionType())
        verify(exactly = 0) { authService.login(any(), any(), any(), any(), any()) }
    }

    @DisplayName("이미 연결된 네이버 회원은 이메일이 없어도 로그인한다")
    @Test
    fun linkedUserLogsInWithoutEmail() {
        // given
        every { authService.isLinked(AuthProvider.NAVER, "n-1") } returns true
        every { authService.login(AuthProvider.NAVER, "n-1", "", "네이버회원", null) } returns token

        // when
        val result = strategy.login(request(email = null))

        // then
        assertSame(token, result)
    }

    @DisplayName("이메일이 있으면 같은 이메일 회원 연결과 새 가입을 하는 기존 로그인 처리에 넘긴다")
    @Test
    fun emailGoesToLogin() {
        // given
        every { authService.isLinked(AuthProvider.NAVER, "n-1") } returns false
        every { authService.login(AuthProvider.NAVER, "n-1", "naver@idolglow.test", "네이버회원", null) } returns token

        // when
        val result = strategy.login(request())

        // then
        assertSame(token, result)
    }

    private fun request(email: String? = "naver@idolglow.test"): LoginRequest {
        val response =
            buildMap<String, Any> {
                put("id", "n-1")
                email?.let { put("email", it) }
                put("nickname", "네이버회원")
            }
        return LoginRequest(attributes = mapOf("resultcode" to "00", "response" to response))
    }
}
