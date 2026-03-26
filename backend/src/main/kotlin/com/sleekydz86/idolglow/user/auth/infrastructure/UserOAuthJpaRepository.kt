package com.sleekydz86.idolglow.user.auth.infrastructure

import com.sleekydz86.idolglow.user.auth.domain.UserOAuth
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import org.springframework.data.jpa.repository.JpaRepository

interface UserOAuthJpaRepository : JpaRepository<UserOAuth, Long> {

    fun findByProviderAndProviderId(
        provider: AuthProvider,
        providerId: String
    ): UserOAuth?

    fun findAllByUserId(userId: Long): List<UserOAuth>

    fun findByEmail(email: String): UserOAuth?
}