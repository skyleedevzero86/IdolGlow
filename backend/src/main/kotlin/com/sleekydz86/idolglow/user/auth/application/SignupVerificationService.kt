package com.sleekydz86.idolglow.user.auth.application

import com.sleekydz86.idolglow.admin.authverification.application.AuthVerificationAuditService
import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import com.sleekydz86.idolglow.user.auth.domain.SignupVerificationStatus
import com.sleekydz86.idolglow.user.auth.domain.SignupVerificationToken
import com.sleekydz86.idolglow.user.auth.domain.SignupVerificationType
import com.sleekydz86.idolglow.user.auth.infrastructure.SignupVerificationTokenRepository
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserAccountStatus
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class SignupVerificationService(
    private val tokenRepository: SignupVerificationTokenRepository,
    private val userRepository: UserRepository,
    private val outboundMailPort: OutboundMailPort,
    private val appMailProperties: AppMailProperties,
    private val authVerificationAuditService: AuthVerificationAuditService,
) {
    private val log = LoggerFactory.getLogger(SignupVerificationService::class.java)

    @Transactional
    fun requestSignupEmailVerification(
        email: String,
        ipAddress: String,
        callbackBaseUrl: String,
    ) {
        val now = LocalDateTime.now()
        val token = UUID.randomUUID().toString().replace("-", "")
        tokenRepository.save(
            SignupVerificationToken(
                token = token,
                type = SignupVerificationType.EMAIL_SIGNUP_VERIFY,
                email = email,
                status = SignupVerificationStatus.PENDING,
                requestedIp = ipAddress,
                expiresAt = now.plusMinutes(5),
                createdAt = now,
            ),
        )
        val confirmUrl = "$callbackBaseUrl/auth/signup/email-verification/confirm?token=$token"
        sendMail(
            to = email,
            subject = "[IdolGlow] 이메일 인증을 완료해 주세요",
            plainTextBody = "아래 링크를 5분 내에 눌러 인증을 완료해 주세요.\n$confirmUrl",
            htmlBody = """
                <p>아래 링크를 <strong>5분 내</strong>에 눌러 이메일 인증을 완료해 주세요.</p>
                <p><a href="$confirmUrl">$confirmUrl</a></p>
            """.trimIndent(),
        )
        authVerificationAuditService.log(
            verificationType = AuthVerificationAuditService.TYPE_SIGNUP_EMAIL_VERIFICATION_REQUEST,
            email = email,
            username = null,
            ipAddress = ipAddress,
            success = true,
            detail = "verification mail requested",
        )
    }

    @Transactional
    fun confirmSignupEmailVerification(tokenValue: String, ipAddress: String): Boolean {
        val token = tokenRepository.findByToken(tokenValue) ?: return false
        val now = LocalDateTime.now()
        if (token.type != SignupVerificationType.EMAIL_SIGNUP_VERIFY) return false
        if (token.status != SignupVerificationStatus.PENDING) return false
        if (token.isExpired(now)) {
            token.status = SignupVerificationStatus.EXPIRED
            token.detail = "expired before confirmation"
            return false
        }
        token.status = SignupVerificationStatus.VERIFIED
        token.confirmedAt = now
        token.confirmedIp = ipAddress
        token.detail = "email verification confirmed"
        authVerificationAuditService.log(
            verificationType = AuthVerificationAuditService.TYPE_SIGNUP_EMAIL_VERIFICATION_CONFIRM,
            email = token.email,
            username = null,
            ipAddress = ipAddress,
            success = true,
            detail = "email verification confirmed",
        )
        return true
    }

    @Transactional
    fun consumeVerifiedSignupToken(email: String) {
        val token = tokenRepository.findTopByEmailAndTypeAndStatusOrderByCreatedAtDesc(
            email = email,
            type = SignupVerificationType.EMAIL_SIGNUP_VERIFY,
            status = SignupVerificationStatus.VERIFIED,
        ) ?: throw IllegalArgumentException("이메일 인증이 완료되지 않았습니다.")
        val now = LocalDateTime.now()
        if (token.confirmedAt == null || token.confirmedAt!!.isBefore(now.minusMinutes(5))) {
            token.status = SignupVerificationStatus.EXPIRED
            token.detail = "verified token expired before signup completion"
            throw IllegalArgumentException("이메일 인증 유효시간(5분)이 만료되었습니다. 다시 인증해 주세요.")
        }
        token.status = SignupVerificationStatus.USED
        token.detail = "used for signup completion"
    }

    @Transactional
    fun sendPostSignupAccountConfirmMail(
        user: User,
        ipAddress: String,
        callbackBaseUrl: String,
    ) {
        val now = LocalDateTime.now()
        val token = UUID.randomUUID().toString().replace("-", "")
        tokenRepository.save(
            SignupVerificationToken(
                token = token,
                type = SignupVerificationType.ACCOUNT_CONFIRM,
                email = user.email,
                username = user.nickname.value,
                userId = user.id,
                status = SignupVerificationStatus.PENDING,
                requestedIp = ipAddress,
                expiresAt = now.plusMinutes(5),
                createdAt = now,
            ),
        )
        val confirmUrl = "$callbackBaseUrl/auth/signup/account-confirm?token=$token&decision=confirm"
        val denyUrl = "$callbackBaseUrl/auth/signup/account-confirm?token=$token&decision=deny"
        sendMail(
            to = user.email,
            subject = "[IdolGlow] 가입 아이디 확인이 필요합니다",
            plainTextBody = "가입한 아이디가 맞으면 확인 링크를 눌러 주세요.\n맞지 않으면 거부 링크를 눌러 계정을 정지할 수 있습니다.\n확인:$confirmUrl\n거부:$denyUrl",
            htmlBody = """
                <p>가입한 아이디(<strong>${user.nickname.value}</strong>)가 맞는지 확인해 주세요.</p>
                <p>유효시간은 <strong>5분</strong>입니다.</p>
                <p><a href="$confirmUrl">맞습니다. 계속 진행</a></p>
                <p><a href="$denyUrl">아닙니다. 계정 정지</a></p>
            """.trimIndent(),
        )
        authVerificationAuditService.log(
            verificationType = AuthVerificationAuditService.TYPE_SIGNUP_ACCOUNT_CONFIRM_REQUEST,
            email = user.email,
            username = user.nickname.value,
            ipAddress = ipAddress,
            success = true,
            detail = "post-signup account confirmation mail sent",
        )
    }

    @Transactional
    fun confirmPostSignupAccount(
        tokenValue: String,
        decision: String,
        ipAddress: String,
    ): Boolean {
        val token = tokenRepository.findByToken(tokenValue) ?: return false
        val now = LocalDateTime.now()
        if (token.type != SignupVerificationType.ACCOUNT_CONFIRM) return false
        if (token.status != SignupVerificationStatus.PENDING) return false
        if (token.isExpired(now)) {
            token.status = SignupVerificationStatus.EXPIRED
            token.detail = "expired before post-signup confirm"
            return false
        }
        val userId = token.userId ?: return false
        val user = userRepository.findById(userId) ?: return false
        val accept = decision.equals("confirm", ignoreCase = true)
        if (accept) {
            token.status = SignupVerificationStatus.USED
            token.detail = "account confirmation accepted"
            user.changeAccountStatus(UserAccountStatus.APPROVED)
        } else {
            token.status = SignupVerificationStatus.REJECTED
            token.detail = "account confirmation denied"
            user.changeAccountStatus(UserAccountStatus.SUSPENDED)
        }
        token.confirmedAt = now
        token.confirmedIp = ipAddress
        userRepository.save(user)
        authVerificationAuditService.log(
            verificationType = AuthVerificationAuditService.TYPE_SIGNUP_ACCOUNT_CONFIRM_RESULT,
            email = token.email,
            username = token.username,
            ipAddress = ipAddress,
            success = accept,
            detail = token.detail,
        )
        return true
    }

    private fun sendMail(
        to: String,
        subject: String,
        plainTextBody: String,
        htmlBody: String,
    ) {
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
