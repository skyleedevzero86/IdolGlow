package com.sleekydz86.idolglow.user.auth.application

import com.sleekydz86.idolglow.admin.authverification.application.AuthVerificationAuditService
import com.sleekydz86.idolglow.global.infrastructure.config.AppMailProperties
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.auth.AuthExceptionType
import com.sleekydz86.idolglow.global.adapter.security.JwtProvider
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

data class PasswordLoginResult(
    val token: TokenResponse,
    val requirePasswordChange: Boolean,
)
