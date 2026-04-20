package com.sleekydz86.idolglow.admin.application

import com.sleekydz86.idolglow.admin.ui.dto.AdminUserPageResponse
import com.sleekydz86.idolglow.admin.ui.dto.AdminUserSummaryResponse
import com.sleekydz86.idolglow.productpackage.admin.application.AdminAuditService
import com.sleekydz86.idolglow.user.auth.domain.UserOAuthRepository
import com.sleekydz86.idolglow.user.user.domain.UserAccountStatus
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminUserService(
    private val userRepository: UserRepository,
    private val userOAuthRepository: UserOAuthRepository,
    private val adminAuditService: AdminAuditService,
) {

    fun findUsers(
        keyword: String?,
        role: UserRole?,
        accountStatus: UserAccountStatus?,
        page: Int,
        size: Int,
    ): AdminUserPageResponse {
        val resolvedPage = page.coerceAtLeast(1)
        val resolvedSize = size.coerceIn(1, 20)
        val result = userRepository.findAllByAdminSearch(
            keyword = keyword,
            role = role,
            accountStatus = accountStatus,
            page = resolvedPage - 1,
            size = resolvedSize,
        )

        return AdminUserPageResponse(
            users = result.items.map { user ->
                AdminUserSummaryResponse.from(user, userOAuthRepository.findAllByUserId(user.id))
            },
            page = resolvedPage,
            size = resolvedSize,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext,
            totalUsers = userRepository.count(),
            adminCount = userRepository.countByRole(UserRole.ADMIN),
            suspendedCount = userRepository.countByAccountStatus(UserAccountStatus.SUSPENDED),
        )
    }

    @Transactional
    fun updateUserRole(userId: Long, role: UserRole): AdminUserSummaryResponse {
        val user = findUser(userId)
        user.changeRole(role)
        val saved = userRepository.save(user)
        adminAuditService.log("USER_ROLE_UPDATE", "USER", saved.id, "role=${saved.role.name}")
        return AdminUserSummaryResponse.from(saved, userOAuthRepository.findAllByUserId(saved.id))
    }

    @Transactional
    fun updateUserStatus(userId: Long, accountStatus: UserAccountStatus): AdminUserSummaryResponse {
        val user = findUser(userId)
        user.changeAccountStatus(accountStatus)
        val saved = userRepository.save(user)
        adminAuditService.log(
            "USER_STATUS_UPDATE",
            "USER",
            saved.id,
            "accountStatus=${saved.accountStatus.name}",
        )
        return AdminUserSummaryResponse.from(saved, userOAuthRepository.findAllByUserId(saved.id))
    }

    @Transactional
    fun unlockUser(userId: Long): AdminUserSummaryResponse {
        val user = findUser(userId)
        user.unlockAccount()
        val saved = userRepository.save(user)
        adminAuditService.log("USER_UNLOCK", "USER", saved.id, "loginFailCount=${saved.loginFailCount}")
        return AdminUserSummaryResponse.from(saved, userOAuthRepository.findAllByUserId(saved.id))
    }

    private fun findUser(userId: Long) =
        userRepository.findById(userId)
            ?: throw EntityNotFoundException("사용자를 찾을 수 없습니다. userId=$userId")
}
