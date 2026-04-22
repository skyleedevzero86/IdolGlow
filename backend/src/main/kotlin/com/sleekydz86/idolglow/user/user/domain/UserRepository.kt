package com.sleekydz86.idolglow.user.user.domain

import com.sleekydz86.idolglow.user.user.domain.vo.UserRole

interface UserRepository {
    fun findById(userId: Long): User?
    fun findByEmail(email: String): User?
    fun findByNicknameValue(nickname: String): User?
    fun findAllByAdminSearch(
        keyword: String?,
        role: UserRole?,
        accountStatus: UserAccountStatus?,
        page: Int,
        size: Int,
    ): UserAdminPage
    fun count(): Long
    fun countByRole(role: UserRole): Long
    fun countByAccountStatus(status: UserAccountStatus): Long
    fun save(user: User): User
    fun saveAndFlush(user: User): User
}

data class UserAdminPage(
    val items: List<User>,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)
