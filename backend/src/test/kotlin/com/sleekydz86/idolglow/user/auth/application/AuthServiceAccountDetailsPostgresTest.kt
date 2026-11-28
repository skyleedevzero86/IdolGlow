package com.sleekydz86.idolglow.user.auth.application

import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import com.sleekydz86.idolglow.user.auth.domain.vo.OAuthAccountDetails
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

@DisplayName("SNS 계정 추가 정보 저장")
class AuthServiceAccountDetailsPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var authService: AuthService

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @DisplayName("네이버 첫 로그인에서 회원이름·성별·연령대를 SNS 연결에 저장한다")
    @Test
    fun savesDetailsOnFirstLogin() {
        // given
        val providerId = "n-${UUID.randomUUID()}"

        // when
        authService.login(AuthProvider.NAVER, providerId, email(), "별명", null, details("홍길동", "F", "20-29"))

        // then
        assertEquals("홍길동|F|20-29", stored(providerId))
    }

    @DisplayName("다시 로그인하면 최신 값으로 덮어쓰고, 네이버가 주지 않은 값은 비운다")
    @Test
    fun overwritesDetailsOnNextLogin() {
        // given
        val providerId = "n-${UUID.randomUUID()}"
        val email = email()
        authService.login(AuthProvider.NAVER, providerId, email, "별명", null, details("홍길동", "F", "20-29"))

        // when
        authService.login(AuthProvider.NAVER, providerId, email, "별명", null, details("홍길순", null, "30-39"))

        // then
        assertEquals("홍길순|null|30-39", stored(providerId))
    }

    @DisplayName("추가 정보를 넘기지 않는 로그인은 저장된 값을 건드리지 않는다")
    @Test
    fun keepsDetailsWhenNotProvided() {
        // given
        val providerId = "n-${UUID.randomUUID()}"
        val email = email()
        authService.login(AuthProvider.NAVER, providerId, email, "별명", null, details("홍길동", "M", "40-49"))

        // when
        authService.login(AuthProvider.NAVER, providerId, email, "별명", null, null)

        // then
        assertEquals("홍길동|M|40-49", stored(providerId))
    }

    private fun details(
        name: String?,
        gender: String?,
        ageRange: String?,
    ) = OAuthAccountDetails(accountName = name, gender = gender, ageRange = ageRange)

    private fun email(): String = "naver-${UUID.randomUUID()}@idolglow.test"

    private fun stored(providerId: String): String =
        jdbcTemplate.queryForObject(
            "select account_name, gender, age_range from user_oauths where provider = 'NAVER' and provider_id = ?",
            { rows, _ -> "${rows.getString(1)}|${rows.getString(2)}|${rows.getString(3)}" },
            providerId,
        )!!
}
