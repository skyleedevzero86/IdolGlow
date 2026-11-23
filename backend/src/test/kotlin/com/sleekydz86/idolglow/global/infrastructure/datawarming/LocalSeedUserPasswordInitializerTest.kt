package com.sleekydz86.idolglow.global.infrastructure.datawarming

import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Optional

@DisplayName("로컬 테스트 사용자 비밀번호 설정")
class LocalSeedUserPasswordInitializerTest {
    private val userJpaRepository = mockk<UserJpaRepository>()
    private val passwordEncoder = BCryptPasswordEncoder()
    private val args = DefaultApplicationArguments()

    @DisplayName("환경 변수가 비어 있으면 아무것도 바꾸지 않는다")
    @Test
    fun doesNothingWhenPasswordIsBlank() {

        // given
        val initializer = LocalSeedUserPasswordInitializer(userJpaRepository, passwordEncoder, "")

        // when
        initializer.run(args)

        // then
        verify(exactly = 0) { userJpaRepository.findById(any()) }
        verify(exactly = 0) { userJpaRepository.save(any()) }
    }

    @DisplayName("환경 변수가 있으면 테스트 사용자 비밀번호를 해시로 저장한다")
    @Test
    fun storesHashedPasswordWhenProvided() {

        // given
        val user = User.of(email = "alice@example.com", nickname = "alice01", role = UserRole.ADMIN)
        every { userJpaRepository.findById(1L) } returns Optional.of(user)
        every { userJpaRepository.save(user) } returns user
        val initializer = LocalSeedUserPasswordInitializer(userJpaRepository, passwordEncoder, "local-test-pw")

        // when
        initializer.run(args)

        // then
        val hash = user.passwordHash.orEmpty()
        assertTrue(hash.isNotEmpty())
        assertTrue(hash != "local-test-pw")
        assertTrue(passwordEncoder.matches("local-test-pw", hash))
        verify(exactly = 1) { userJpaRepository.save(user) }
    }

    @DisplayName("이미 같은 비밀번호면 다시 저장하지 않는다")
    @Test
    fun skipsWhenPasswordAlreadyMatches() {

        // given
        val user = User.of(email = "alice@example.com", nickname = "alice01")
        user.applyEncodedPasswordChange(requireNotNull(passwordEncoder.encode("local-test-pw")))
        val changeCountBefore = user.passwordChangeDailyCount
        every { userJpaRepository.findById(1L) } returns Optional.of(user)
        val initializer = LocalSeedUserPasswordInitializer(userJpaRepository, passwordEncoder, "local-test-pw")

        // when
        initializer.run(args)

        // then
        assertEquals(changeCountBefore, user.passwordChangeDailyCount)
        verify(exactly = 0) { userJpaRepository.save(any()) }
    }

    @DisplayName("테스트 사용자가 없으면 저장하지 않는다")
    @Test
    fun skipsWhenSeedUserMissing() {

        // given
        every { userJpaRepository.findById(1L) } returns Optional.empty()
        val initializer = LocalSeedUserPasswordInitializer(userJpaRepository, passwordEncoder, "local-test-pw")

        // when
        initializer.run(args)

        // then
        verify(exactly = 0) { userJpaRepository.save(any()) }
    }
}
