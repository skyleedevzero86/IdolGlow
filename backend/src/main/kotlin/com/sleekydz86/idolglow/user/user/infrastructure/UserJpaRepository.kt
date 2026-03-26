package com.sleekydz86.idolglow.user.user.infrastructure

import com.sleekydz86.idolglow.user.user.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
}
