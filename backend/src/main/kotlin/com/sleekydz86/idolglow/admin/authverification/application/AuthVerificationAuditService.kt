package com.sleekydz86.idolglow.admin.authverification.application

import com.sleekydz86.idolglow.admin.authverification.domain.AuthVerificationAuditLog
import com.sleekydz86.idolglow.admin.authverification.infrastructure.AuthVerificationAuditLogRepository
import com.sleekydz86.idolglow.admin.authverification.ui.dto.AuthVerificationAuditLogPageResponse
import com.sleekydz86.idolglow.admin.authverification.ui.dto.AuthVerificationAuditLogResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthVerificationAuditService(
    private val authVerificationAuditLogRepository: AuthVerificationAuditLogRepository,
) {
    @Transactional
    fun log(
        verificationType: String,
        email: String?,
        username: String?,
        ipAddress: String,
        success: Boolean,
        detail: String?,
    ) {
        authVerificationAuditLogRepository.save(
            AuthVerificationAuditLog(
                verificationType = verificationType,
                email = email?.trim()?.takeIf { it.isNotBlank() },
                username = username?.trim()?.takeIf { it.isNotBlank() },
                ipAddress = ipAddress.ifBlank { "unknown" }.take(64),
                success = success,
                detail = detail?.take(500),
                createdAt = LocalDateTime.now(),
            ),
        )
    }

    @Transactional(readOnly = true)
    fun findPage(
        page: Int,
        size: Int,
        verificationType: String?,
        keyword: String?,
    ): AuthVerificationAuditLogPageResponse {
        val pageable = PageRequest.of(
            page.coerceAtLeast(1) - 1,
            size.coerceIn(1, 100),
            Sort.by(Sort.Direction.DESC, "createdAt"),
        )
        val queryType = verificationType?.trim().orEmpty()
        val queryKeyword = keyword?.trim()
        val data = if (queryType.isBlank()) {
            authVerificationAuditLogRepository.searchByKeyword(queryKeyword, pageable)
        } else {
            authVerificationAuditLogRepository.searchByTypeAndKeyword(queryType, queryKeyword, pageable)
        }
        return AuthVerificationAuditLogPageResponse(
            logs = data.content.map(AuthVerificationAuditLogResponse::from),
            page = data.number + 1,
            size = data.size,
            totalElements = data.totalElements,
            totalPages = data.totalPages.coerceAtLeast(1),
        )
    }

    companion object {
        const val TYPE_SIGNUP_EMAIL_CHECK = "SIGNUP_EMAIL_CHECK"
        const val TYPE_ACCOUNT_RECOVERY_INITIATE = "ACCOUNT_RECOVERY_INITIATE"
    }
}
