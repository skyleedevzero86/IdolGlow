package com.sleekydz86.idolglow.user.user.domain

import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.UUID

@DisplayName("회원 가입 시각")
class UserJoinedAtPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var userRepository: UserRepository

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var transactionTemplate: TransactionTemplate

    @DisplayName("새 회원을 저장하면 가입 시각이 채워지고, 닉네임 변경·로그인 뒤에도 바뀌지 않는다")
    @Test
    fun assignsJoinedAtOnceOnFirstSave() {
        // given
        val beforeSave = LocalDateTime.now().minusSeconds(1)
        val saved = userRepository.save(newUser())
        val joinedAt = storedJoinedAt(saved.id)

        // when
        transactionTemplate.executeWithoutResult {
            val loaded = requireNotNull(userRepository.findById(saved.id))
            loaded.updateNickname("바뀐${saved.id}".take(10))
            loaded.updateLastLoginTime(LocalDateTime.now().plusDays(3))
        }

        // then
        assertNotNull(joinedAt)
        assertEquals(true, joinedAt!!.isAfter(beforeSave))
        assertEquals(joinedAt, storedJoinedAt(saved.id))
    }

    @DisplayName("가입 시각이 없던 회원은 정보를 바꿔 저장해도 계속 비어 있다")
    @Test
    fun keepsMissingJoinedAtEmpty() {
        // given
        val saved = userRepository.save(newUser())
        jdbcTemplate.update("update users set joined_at = null where id = ?", saved.id)

        // when
        transactionTemplate.executeWithoutResult {
            val loaded = requireNotNull(userRepository.findById(saved.id))
            loaded.updateLastLoginTime(LocalDateTime.now())
        }

        // then
        assertNull(storedJoinedAt(saved.id))
    }

    private fun newUser(): User {
        val token =
            UUID
                .randomUUID()
                .toString()
                .replace("-", "")
                .take(8)
        return User(email = "j$token@test.com", nickname = Nickname.of("j$token"))
    }

    private fun storedJoinedAt(userId: Long): LocalDateTime? =
        jdbcTemplate.queryForObject("select joined_at from users where id = ?", LocalDateTime::class.java, userId)
}
