package com.sleekydz86.idolglow.platform.auth.service

import com.sleekydz86.idolglow.platform.auth.domain.JwtToken
import com.sleekydz86.idolglow.platform.auth.dto.LoginRequest
import com.sleekydz86.idolglow.platform.auth.dto.RefreshTokenRequest
import com.sleekydz86.idolglow.platform.auth.dto.UserRegistrationRequest
import com.sleekydz86.idolglow.platform.auth.util.JwtTokenUtil
import com.sleekydz86.idolglow.platform.user.domain.PlatformUser
import com.sleekydz86.idolglow.platform.user.domain.PlatformUserRole
import com.sleekydz86.idolglow.platform.user.domain.PlatformUserStatus
import com.sleekydz86.idolglow.platform.user.exception.AuthenticationFailedException
import com.sleekydz86.idolglow.platform.user.exception.InvalidTokenException
import com.sleekydz86.idolglow.platform.user.exception.UserAlreadyExistsException
import com.sleekydz86.idolglow.platform.user.exception.UserNotFoundException
import com.sleekydz86.idolglow.platform.user.password.PasswordPolicyValidator
import com.sleekydz86.idolglow.platform.user.port.PlatformUserAccountPort
import com.sleekydz86.idolglow.global.exceptions.CustomException
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@ConditionalOnProperty(prefix = "platform.auth", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class AuthenticationService(
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenUtil: JwtTokenUtil,
    private val userAccountPort: PlatformUserAccountPort,
    private val passwordEncoder: PasswordEncoder,
    private val passwordPolicyValidator: PasswordPolicyValidator,
) {

    private val log = LoggerFactory.getLogger(AuthenticationService::class.java)

    @Transactional
    fun login(request: LoginRequest): JwtToken {
        try {
            val validationResult = passwordPolicyValidator.validate(request.password)
            if (!validationResult.valid) {
                log.warn("비밀번호 정책 검증 실패: {}", validationResult.errors)
                throw AuthenticationFailedException(request.email)
            }

            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.email, request.password),
            )
            SecurityContextHolder.getContext().authentication = authentication

            val user = userAccountPort.findByEmail(request.email)
                .orElseThrow { UserNotFoundException(request.email) }

            val accessToken = jwtTokenUtil.generateAccessToken(request.email, user.role)
            val refreshToken = jwtTokenUtil.generateRefreshToken(request.email)

            updateLastLoginTime(request.email)

            log.info("로그인 성공: {}", request.email)

            return JwtToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(LocalDateTime.now().plusSeconds(jwtTokenUtil.getAccessTokenExpirationMillis() / 1000))
                .build()
        } catch (e: Exception) {
            log.error("로그인 실패: {}", e.message, e)
            throw AuthenticationFailedException(request.email)
        }
    }

    @Transactional
    fun refresh(request: RefreshTokenRequest): JwtToken {
        log.debug("토큰 갱신 시도")
        try {
            if (!jwtTokenUtil.validateRefreshToken(request.refreshToken)) {
                log.warn("유효하지 않은 리프레시 토큰")
                throw InvalidTokenException("REFRESH")
            }

            val email = jwtTokenUtil.getEmailFromRefreshToken(request.refreshToken)
                ?: throw InvalidTokenException("REFRESH")

            if (!userAccountPort.existsByEmail(email)) {
                log.warn("리프레시 토큰에 해당하는 사용자 없음: {}", email)
                throw InvalidTokenException("REFRESH")
            }

            val user = userAccountPort.findByEmail(email)
                .orElseThrow { UserNotFoundException(email) }

            val newAccessToken = jwtTokenUtil.generateAccessToken(email, user.role)
            val newRefreshToken = jwtTokenUtil.generateRefreshToken(email)

            log.info("토큰 갱신 성공: {}", email)

            return JwtToken.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(LocalDateTime.now().plusSeconds(jwtTokenUtil.getAccessTokenExpirationMillis() / 1000))
                .build()
        } catch (e: Exception) {
            log.error("토큰 갱신 실패: {}", e.message)
            throw InvalidTokenException("REFRESH")
        }
    }

    @Transactional
    fun register(request: UserRegistrationRequest): PlatformUser {
        log.info("회원가입 시도: {}", request.email)

        if (userAccountPort.existsByEmail(request.email)) {
            log.warn("이미 등록된 이메일: {}", request.email)
            throw UserAlreadyExistsException(request.email)
        }

        val validationResult = passwordPolicyValidator.validate(request.password)
        if (!validationResult.valid) {
            log.warn("비밀번호 정책 검증 실패: {}", validationResult.errors)
            throw IllegalArgumentException(
                "비밀번호 정책을 만족하지 않습니다: " + validationResult.errors.joinToString(", "),
            )
        }

        val nicknameForDisplay: String = try {
            val rawNick = request.nickname?.trim()?.takeIf { it.isNotBlank() } ?: request.username.trim()
            Nickname.of(rawNick).value
        } catch (_: CustomException) {
            Nickname.defaultFromEmail(request.email.trim()).value
        }

        val user = PlatformUser.builder()
            .email(request.email.trim().lowercase())
            .password(requireNotNull(passwordEncoder.encode(request.password.trim())) { "비밀번호 인코딩에 실패했습니다." })
            .username(request.username.trim())
            .nickname(nicknameForDisplay)
            .role(PlatformUserRole.USER)
            .status(PlatformUserStatus.APPROVED)
            .passwordChangedAt(LocalDateTime.now())
            .build()

        val saved = userAccountPort.save(user)
        log.info("회원가입 완료: {} (id={})", request.email, saved.id)
        return saved
    }

    private fun updateLastLoginTime(email: String) {
        val opt = userAccountPort.findByEmail(email)
        if (opt.isPresent) {
            val user = opt.get()
            user.updateLastLoginAt(LocalDateTime.now())
            userAccountPort.save(user)
        }
    }

    @Transactional
    fun authenticate(email: String, password: String): JwtToken {
        try {
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(email, password),
            )

            val userDetails = authentication.principal as UserDetails
            val authority = userDetails.authorities.firstOrNull()?.authority
                ?: throw IllegalArgumentException("권한 정보가 없습니다.")
            val roleName = if (authority.startsWith("ROLE_")) authority.substring("ROLE_".length) else authority
            val role = PlatformUserRole.valueOf(roleName)

            val accessToken = jwtTokenUtil.generateAccessToken(email, role)
            val refreshToken = jwtTokenUtil.generateRefreshToken(email)

            updateLastLoginTime(email)

            return JwtToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(LocalDateTime.now().plusSeconds(jwtTokenUtil.getAccessTokenExpirationMillis() / 1000))
                .build()
        } catch (e: Exception) {
            log.error("인증 처리 실패: {}", email, e)
            throw AuthenticationFailedException(email)
        }
    }
}
