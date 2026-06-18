package com.sleekydz86.idolglow.global.adapter.resolver

import com.sleekydz86.idolglow.global.adapter.security.AuthChannel
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.auth.AuthExceptionType
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

class AuthenticatedPrincipalResolverTest {
    private val userRepository = mockk<UserRepository>()
    private val resolver = AuthenticatedPrincipalResolver(userRepository)

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `global_jwt_주체_userId_를_해석한다`() {
        // given
        setAuthentication(
            UsernamePasswordAuthenticationToken(
                "42",
                "",
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            ),
        )

        // when
        val principal = resolver.resolveRequired()

        // then
        assertEquals(AuthChannel.GLOBAL, principal.channel)
        assertEquals(42L, principal.accountId)
        assertEquals(42L, principal.userId)
        assertEquals(setOf("USER"), principal.roles)
    }

    @Test
    fun `이메일_주체는_user_테이블에서_userId_를_조회한다`() {
        // given
        every { userRepository.findByEmail("member@example.com") } returns user(id = 7L, email = "member@example.com")
        setAuthentication(
            UsernamePasswordAuthenticationToken(
                "member@example.com",
                "",
                listOf(SimpleGrantedAuthority("ROLE_ADMIN")),
            ),
        )

        // when
        val principal = resolver.resolveRequired()

        // then
        assertEquals(AuthChannel.PLATFORM, principal.channel)
        assertEquals(7L, principal.userId)
        assertEquals("member@example.com", principal.email)
    }

    @Test
    fun `매핑되지_않은_이메일_주체는_userId_조회에_실패한다`() {
        // given
        every { userRepository.findByEmail("unknown@example.com") } returns null
        setAuthentication(
            UsernamePasswordAuthenticationToken(
                "unknown@example.com",
                "",
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            ),
        )

        // when / then
        assertThrows(CustomException::class.java) {
            resolver.requireUserId()
        }
        assertNull(resolver.resolveOrNull())
    }

    private fun setAuthentication(authentication: UsernamePasswordAuthenticationToken) {
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun user(
        id: Long,
        email: String,
    ): User =
        User(
            id = id,
            email = email,
            nickname = com.sleekydz86.idolglow.user.user.domain.vo.Nickname("tester"),
            role = UserRole.USER,
        )
}
