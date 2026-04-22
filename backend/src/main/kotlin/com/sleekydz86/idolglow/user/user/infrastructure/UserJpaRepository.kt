package com.sleekydz86.idolglow.user.user.infrastructure

import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserAccountStatus
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserJpaRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?

    @Query("SELECT u FROM User u WHERE u.nickname.value = :nickname")
    fun findByNicknameValue(@Param("nickname") nickname: String): User?

    fun findByEmailAndPlatformUsername(email: String, platformUsername: String): User?

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.nickname.value = :username")
    fun findByEmailAndNicknameValue(
        @Param("email") email: String,
        @Param("username") username: String,
    ): User?

    @Query(
        """
        SELECT u
        FROM User u
        WHERE (:keyword IS NULL OR :keyword = '' OR
          lower(u.email) LIKE lower(concat('%', :keyword, '%')) OR
          lower(u.nickname.value) LIKE lower(concat('%', :keyword, '%')))
          AND (:role IS NULL OR u.role = :role)
          AND (:accountStatus IS NULL OR u.accountStatus = :accountStatus)
        """
    )
    fun searchAdminUsers(
        @Param("keyword") keyword: String?,
        @Param("role") role: UserRole?,
        @Param("accountStatus") accountStatus: UserAccountStatus?,
        pageable: Pageable,
    ): Page<User>

    fun countByRole(role: UserRole): Long

    fun countByAccountStatus(accountStatus: UserAccountStatus): Long
}
