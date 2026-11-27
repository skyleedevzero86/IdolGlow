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

@DisplayName("카카오 로그인 전략")
class KakaoLoginStrategyTest {
    private val authService = mockk<AuthService>()
    private val strategy = KakaoLoginStrategy(authService)
    private val token = mockk<TokenResponse>()

    @DisplayName("K4 연결이 없고 쓸 수 있는 이메일도 없으면 KAKAO_EMAIL_REQUIRED 로 거절하고 회원을 만들지 않는다")
    @Test
    fun rejectsNewUserWithoutUsableEmail() {
        // given
        every { authService.isLinked(AuthProvider.KAKAO, "77") } returns false

        // when
        val error = assertThrows(CustomException::class.java) { strategy.login(request(verified = false)) }

        // then
        assertEquals(AuthExceptionType.KAKAO_EMAIL_REQUIRED, error.getExceptionType())
        verify(exactly = 0) { authService.login(any(), any(), any(), any(), any()) }
    }

    @DisplayName("K2 이미 연결된 카카오 회원은 이메일이 없어도 로그인한다")
    @Test
    fun linkedUserLogsInWithoutEmail() {
        // given
        every { authService.isLinked(AuthProvider.KAKAO, "77") } returns true
        every { authService.login(AuthProvider.KAKAO, "77", "", "응모회원", null) } returns token

        // when
        val result = strategy.login(request(email = null))

        // then
        assertSame(token, result)
    }

    @DisplayName("K5·K6 쓸 수 있는 이메일이 있으면 그 이메일로 기존 로그인 처리에 넘긴다")
    @Test
    fun usableEmailGoesToLogin() {
        // given
        every { authService.isLinked(AuthProvider.KAKAO, "77") } returns false
        every { authService.login(AuthProvider.KAKAO, "77", "kakao@idolglow.test", "응모회원", null) } returns token

        // when
        val result = strategy.login(request())

        // then
        assertSame(token, result)
    }

    private fun request(
        email: String? = "kakao@idolglow.test",
        verified: Boolean = true,
    ): LoginRequest {
        val account =
            buildMap<String, Any> {
                email?.let { put("email", it) }
                put("is_email_valid", true)
                put("is_email_verified", verified)
                put("profile", mapOf("nickname" to "응모회원"))
            }
        return LoginRequest(attributes = mapOf("id" to 77L, "kakao_account" to account))
    }
}
