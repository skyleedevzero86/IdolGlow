package com.sleekydz86.idolglow.user.auth.application

import com.sleekydz86.idolglow.admin.authverification.application.AuthVerificationAuditService
import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.global.exceptions.CustomException
import com.sleekydz86.idolglow.global.exceptions.auth.AuthExceptionType
import com.sleekydz86.idolglow.global.security.JwtProvider
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserAccountStatus
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime

@Service
class PasswordRecoveryService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val outboundMailPort: OutboundMailPort,
    private val appMailProperties: AppMailProperties,
    private val authVerificationAuditService: AuthVerificationAuditService,
) {
    private val log = LoggerFactory.getLogger(PasswordRecoveryService::class.java)
    private val random = SecureRandom()

    @Transactional
    fun issueTemporaryPassword(email: String, ipAddress: String): Boolean {
        val normalized = email.trim().lowercase()
        if (normalized.isBlank()) return false
        val user = userRepository.findByEmail(normalized) ?: return false
        if (user.passwordHash.isNullOrBlank()) return false

        val temporaryPassword = generateTemporaryPassword()
        val encoded = requireNotNull(passwordEncoder.encode(temporaryPassword)) { "임시 비밀번호 인코딩에 실패했습니다." }
        user.issueTemporaryPassword(encoded, LocalDateTime.now())
        userRepository.save(user)

        sendMail(
            to = user.email,
            subject = "[IdolGlow] 임시 비밀번호가 발급되었습니다",
            plainTextBody = "임시 비밀번호: $temporaryPassword\n로그인 후 반드시 비밀번호를 변경해 주세요.",
            htmlBody = """
                <p>임시 비밀번호가 발급되었습니다.</p>
                <p><strong>$temporaryPassword</strong></p>
                <p>로그인 후 즉시 새 비밀번호로 변경해 주세요.</p>
            """.trimIndent(),
        )
        authVerificationAuditService.log(
            verificationType = AuthVerificationAuditService.TYPE_PASSWORD_TEMP_ISSUED,
            email = user.email,
            username = user.nickname.value,
            ipAddress = ipAddress,
            success = true,
            detail = "temporary password issued",
        )
        return true
    }

    @Transactional(readOnly = true)
    fun sendAccountIdReminder(email: String, ipAddress: String): Boolean {
        val normalized = email.trim().lowercase()
        if (normalized.isBlank()) return false
        val user = userRepository.findByEmail(normalized) ?: return false

        sendMail(
            to = user.email,
            subject = "[IdolGlow] 아이디 찾기 결과 안내",
            plainTextBody = "요청하신 계정의 아이디는 '${user.nickname.value}' 입니다.",
            htmlBody = """
                <p>요청하신 계정의 아이디는 아래와 같습니다.</p>
                <p><strong>${user.nickname.value}</strong></p>
            """.trimIndent(),
        )
        authVerificationAuditService.log(
            verificationType = AuthVerificationAuditService.TYPE_ACCOUNT_ID_FIND,
            email = user.email,
            username = user.nickname.value,
            ipAddress = ipAddress,
            success = true,
            detail = "account id reminder sent",
        )
        return true
    }

    @Transactional
    fun loginWithPassword(email: String, password: String): PasswordLoginResult {
        val normalized = email.trim().lowercase()
        val user = userRepository.findByEmail(normalized)
            ?: throw CustomException(AuthExceptionType.UNAUTHENTICATED)
        val hash = user.passwordHash ?: throw CustomException(AuthExceptionType.UNAUTHENTICATED)
        if (!passwordEncoder.matches(password, hash)) {
            throw CustomException(AuthExceptionType.UNAUTHENTICATED)
        }
        if (user.accountStatus == UserAccountStatus.SUSPENDED) {
            throw IllegalArgumentException("정지된 계정입니다.")
        }

        user.updateLastLoginTime()
        userRepository.save(user)
        val token = jwtProvider.generateToken(user.id, user.role)
        return PasswordLoginResult(token, user.temporaryPasswordRequired)
    }

    @Transactional
    fun markPasswordChanged(user: User, ipAddress: String) {
        authVerificationAuditService.log(
            verificationType = AuthVerificationAuditService.TYPE_PASSWORD_CHANGED,
            email = user.email,
            username = user.nickname.value,
            ipAddress = ipAddress,
            success = true,
            detail = "password changed after temporary login",
        )
    }

    private fun generateTemporaryPassword(length: Int = 12): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$"
        return buildString(length) {
            repeat(length) {
                append(chars[random.nextInt(chars.length)])
            }
        }
    }

    private fun sendMail(to: String, subject: String, plainTextBody: String, htmlBody: String) {
        if (!appMailProperties.enabled) {
            log.warn("메일 기능 비활성화 상태입니다. to={}, subject={}", to, subject)
            return
        }
        outboundMailPort.send(
            OutboundMailMessage(
                to = to,
                subject = subject,
                plainTextBody = plainTextBody,
                htmlBody = htmlBody,
            ),
        )
    }
}

data class PasswordLoginResult(
    val token: TokenResponse,
    val requirePasswordChange: Boolean,
)
