package com.sleekydz86.idolglow.user.auth.infrastructure

import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.auth.domain.UserOAuth
import com.sleekydz86.idolglow.user.auth.domain.UserOAuthRepository
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

@DisplayName("SNS 연결 중복 금지")
class UserOAuthUniquePostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var userOAuthRepository: UserOAuthRepository

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @DisplayName("같은 제공자의 같은 회원번호로 두 번째 연결을 저장하면 거절하고 한 줄만 남긴다")
    @Test
    fun rejectsSecondLinkForSameAccount() {
        // given
        val providerId = "n-${UUID.randomUUID()}"
        userOAuthRepository.save(link(AuthProvider.NAVER, providerId))

        // when
        assertThrows(DataIntegrityViolationException::class.java) {
            userOAuthRepository.save(link(AuthProvider.NAVER, providerId))
        }

        // then
        assertEquals(1, rows(AuthProvider.NAVER, providerId))
    }

    @DisplayName("다른 제공자는 같은 회원번호를 따로 연결할 수 있다")
    @Test
    fun allowsSameIdForOtherProvider() {
        // given
        val providerId = "same-${UUID.randomUUID()}"
        userOAuthRepository.save(link(AuthProvider.NAVER, providerId))

        // when
        userOAuthRepository.save(link(AuthProvider.KAKAO, providerId))

        // then
        assertEquals(1, rows(AuthProvider.NAVER, providerId))
        assertEquals(1, rows(AuthProvider.KAKAO, providerId))
    }

    private fun link(
        provider: AuthProvider,
        providerId: String,
    ): UserOAuth =
        UserOAuth.of(userId = 1, provider = provider, providerId = providerId, email = "$providerId@idolglow.test")

    private fun rows(
        provider: AuthProvider,
        providerId: String,
    ): Int =
        jdbcTemplate.queryForObject(
            "select count(*) from user_oauths where provider = ? and provider_id = ?",
            Int::class.java,
            provider.name,
            providerId,
        )!!
}
