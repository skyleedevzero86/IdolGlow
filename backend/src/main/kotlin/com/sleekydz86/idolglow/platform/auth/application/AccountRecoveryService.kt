package com.sleekydz86.idolglow.platform.auth.application

import com.sleekydz86.idolglow.platform.auth.config.PlatformAuthProperties
import com.sleekydz86.idolglow.platform.auth.application.dto.AccountRecoveryRequest
import com.sleekydz86.idolglow.platform.auth.application.dto.AccountRecoveryResponse
import com.sleekydz86.idolglow.platform.auth.application.dto.PasswordResetRequest
import com.sleekydz86.idolglow.platform.auth.util.JwtTokenUtil
import com.sleekydz86.idolglow.platform.user.domain.exception.UserNotFoundException
import com.sleekydz86.idolglow.platform.user.password.PasswordPolicyValidator
import com.sleekydz86.idolglow.platform.user.port.PlatformUserAccountPort
import com.sleekydz86.idolglow.platform.user.recovery.RecoveryJtiStore
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@ConditionalOnProperty(prefix = "platform.auth", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class AccountRecoveryService(
    private val userAccountPort: PlatformUserAccountPort,
    private val jwtTokenUtil: JwtTokenUtil,
    private val recoveryJtiStore: RecoveryJtiStore,
    private val passwordEncoder: PasswordEncoder,
    private val passwordPolicyValidator: PasswordPolicyValidator,
    private val properties: PlatformAuthProperties,
) {

    private val log = LoggerFactory.getLogger(AccountRecoveryService::class.java)

    @Transactional(readOnly = true)
    fun initiate(request: AccountRecoveryRequest): AccountRecoveryResponse {
        userAccountPort.findByEmailAndUsername(request.email, request.username)
            .orElseThrow { UserNotFoundException(request.email) }

        val jti = UUID.randomUUID().toString()
        recoveryJtiStore.register(jti, properties.jwt.recoveryTokenTtl)
        val token = jwtTokenUtil.generateRecoveryToken(request.email, request.username, jti)

        log.info("복구 토큰 발급: email={}, username={}", request.email, request.username)

        return AccountRecoveryResponse.builder()
            .success(true)
            .message("복구 토큰이 발급되었습니다. 안전한 채널로 사용자에게 전달하세요.")
            .recoveryToken(token)
            .expiresIn(properties.jwt.recoveryTokenTtl.toSeconds())
            .build()
    }

    @Transactional
    fun resetPassword(request: PasswordResetRequest): AccountRecoveryResponse {
        if (!jwtTokenUtil.validateRecoveryToken(request.recoveryToken)) {
            throw IllegalArgumentException("유효하지 않거나 만료된 복구 토큰입니다.")
        }

        val jti = jwtTokenUtil.getJtiFromRecoveryToken(request.recoveryToken)
        if (jti == null || !recoveryJtiStore.consume(jti)) {
            throw IllegalArgumentException("이미 사용되었거나 유효하지 않은 복구 토큰입니다.")
        }

        val userInfo = jwtTokenUtil.getUserInfoFromRecoveryToken(request.recoveryToken)
        val parts = userInfo.split(":", limit = 2)
        if (parts.size != 2) {
            throw IllegalArgumentException("유효하지 않은 복구 토큰 페이로드입니다.")
        }

        val user = userAccountPort.findByEmailAndUsername(parts[0], parts[1])
            .orElseThrow { UserNotFoundException(parts[0]) }

        if (request.newPassword != request.confirmPassword) {
            throw IllegalArgumentException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.")
        }

        val validation = passwordPolicyValidator.validate(request.newPassword)
        if (!validation.valid) {
            throw IllegalArgumentException("비밀번호 정책을 만족하지 않습니다: " + validation.errors.joinToString(", "))
        }

        val newPwd = requireNotNull(request.newPassword) { "새 비밀번호가 필요합니다." }.trim()
        val encoded: String = requireNotNull(passwordEncoder.encode(newPwd)) { "비밀번호 인코딩에 실패했습니다." }
        user.changePassword(encoded)
        userAccountPort.save(user)

        log.info("복구 토큰으로 비밀번호 재설정: userId={}", user.id ?: "알 수 없음")

        return AccountRecoveryResponse.builder()
            .success(true)
            .message("비밀번호가 재설정되었습니다.")
            .build()
    }
}
