package com.sleekydz86.idolglow.global.adapter.security

import com.sleekydz86.idolglow.platform.auth.config.PlatformAuthProperties
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("서버 재시작 후 로그인 토큰")
class JwtProviderProcessEpochTest {
    @Test
    @DisplayName("같은 프로세스에서 발급한 토큰은 유효하다")
    fun acceptsTokenFromSameProcess() {
        // given
        val provider = provider()
        val token = provider.generateToken(7L, UserRole.USER)

        // when
        val valid = provider.validateAccessToken(token.accessToken)

        // then
        assertTrue(valid)
    }

    @Test
    @DisplayName("서버를 다시 띄우면 이전 토큰은 거절한다")
    fun rejectsTokenFromPreviousProcess() {
        // given
        val previous = provider()
        val restarted = provider()
        val token = previous.generateToken(7L, UserRole.USER)

        // when
        val accessValid = restarted.validateAccessToken(token.accessToken)
        val refreshValid = restarted.validateRefreshToken(token.refreshToken)

        // then
        assertFalse(accessValid)
        assertFalse(refreshValid)
    }

    private fun provider(): JwtProvider {
        val properties = PlatformAuthProperties()
        properties.jwt.secret = SECRET_HEAD + SECRET_TAIL
        return JwtProvider(properties)
    }

    companion object {
        private const val SECRET_HEAD = "defaultSecretKeyForPhase1TestsOnly"
        private const val SECRET_TAIL = "123456789012345678901234567890123456789012345678901234567890"
    }
}
