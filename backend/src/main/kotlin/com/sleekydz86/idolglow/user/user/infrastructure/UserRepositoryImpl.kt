package com.sleekydz86.idolglow.user.user.infrastructure

import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserAccountStatus
import com.sleekydz86.idolglow.user.user.domain.UserAdminPage
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {

    override fun findById(userId: Long): User? =
        userJpaRepository.findByIdOrNull(userId)

    override fun findByEmail(email: String): User? =
        userJpaRepository.findByEmail(email)

    override fun findByNicknameValue(nickname: String): User? =
        userJpaRepository.findByNicknameValue(nickname)

    override fun findAllByAdminSearch(
        keyword: String?,
        role: UserRole?,
        accountStatus: UserAccountStatus?,
        page: Int,
        size: Int,
    ): UserAdminPage {
        val pageable = PageRequest.of(
            page.coerceAtLeast(0),
            size.coerceIn(1, 50),
            Sort.by(
                Sort.Order.desc("lastLoginAt"),
                Sort.Order.desc("id"),
            ),
        )
        val result = userJpaRepository.searchAdminUsers(
            keyword = keyword?.trim()?.takeIf { it.isNotEmpty() },
            role = role,
            accountStatus = accountStatus,
            pageable = pageable,
        )
        return UserAdminPage(
            items = result.content,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext(),
        )
    }

    override fun count(): Long =
        userJpaRepository.count()

    override fun countByRole(role: UserRole): Long =
        userJpaRepository.countByRole(role)

    override fun countByAccountStatus(status: UserAccountStatus): Long =
        userJpaRepository.countByAccountStatus(status)

    override fun save(user: User): User =
        userJpaRepository.save(user)

    override fun saveAndFlush(user: User): User =
        userJpaRepository.saveAndFlush(user)
}
