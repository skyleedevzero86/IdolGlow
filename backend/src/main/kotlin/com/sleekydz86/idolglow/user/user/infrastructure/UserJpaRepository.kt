package com.sleekydz86.idolglow.user.user.infrastructure

import com.sleekydz86.idolglow.user.user.domain.User
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
}
