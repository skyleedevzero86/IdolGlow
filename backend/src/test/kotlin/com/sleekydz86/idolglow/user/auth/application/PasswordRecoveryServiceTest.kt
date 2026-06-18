package com.sleekydz86.idolglow.user.auth.application

import com.sleekydz86.idolglow.admin.authverification.application.AuthVerificationAuditService
import com.sleekydz86.idolglow.global.adapter.security.JwtProvider
import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.auth.AuthExceptionType
import com.sleekydz86.idolglow.platform.auth.config.PlatformAuthProperties
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserAccountStatus
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class PasswordRecoveryServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = BCryptPasswordEncoder()
    private val outboundMailPort = mockk<OutboundMailPort>(relaxed = true)
    private val appMailProperties = AppMailProperties(enabled = false)
    private val authVerificationAuditService = mockk<AuthVerificationAuditService>(relaxed = true)
    private val jwtProvider =
        JwtProvider(
            PlatformAuthProperties().apply {
                jwt.secret = "defaultSecretKeyForPhase1TestsOnly123456789012345678901234567890123456789012345678901234567890"
            },
        )

    private lateinit var service: PasswordRecoveryService

    @BeforeEach
    fun setUp() {
        service =
            PasswordRecoveryService(
                userRepository = userRepository,
                passwordEncoder = passwordEncoder,
                jwtProvider = jwtProvider,
                outboundMailPort = outboundMailPort,
                appMailProperties = appMailProperties,
                authVerificationAuditService = authVerificationAuditService,
            )
    }

    @Test
    fun `비밀번호가_일치하면_로그인_토큰을_발급한다`() {
        // given
        val rawPassword = "ValidPass!234"
        val user =
            User(
                id = 11L,
                email = "fan@example.com",
                nickname = Nickname.of("glowfan"),
                passwordHash = passwordEncoder.encode(rawPassword),
                role = UserRole.USER,
                accountStatus = UserAccountStatus.APPROVED,
            )
        every { userRepository.findByEmail("fan@example.com") } returns user
        every { userRepository.save(user) } returns user

        // when
        val result = service.loginWithPassword("fan@example.com", rawPassword)

        // then
        assertFalse(result.requirePasswordChange)
        assertTrue(result.token.accessToken.isNotBlank())
        assertTrue(jwtProvider.validateAccessToken(result.token.accessToken))
        verify(exactly = 1) { userRepository.save(user) }
    }

    @Test
    fun `비밀번호가_틀리면_인증에_실패한다`() {
        // given
        val user =
            User(
                id = 11L,
                email = "fan@example.com",
                nickname = Nickname.of("glowfan"),
                passwordHash = passwordEncoder.encode("ValidPass!234"),
                role = UserRole.USER,
            )
        every { userRepository.findByEmail("fan@example.com") } returns user

        // when / then
        val exception =
            assertThrows(CustomException::class.java) {
                service.loginWithPassword("fan@example.com", "WrongPass!234")
            }
        assertEquals(AuthExceptionType.UNAUTHENTICATED, exception.getExceptionType())
    }
}
