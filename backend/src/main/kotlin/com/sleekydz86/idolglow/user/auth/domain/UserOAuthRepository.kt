package com.sleekydz86.idolglow.user.auth.domain

import java.security.AuthProvider

interface UserOAuthRepository {
    fun save(userOAuth: UserOAuth): UserOAuth
    fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): UserOAuth?
    fun findAllByUserId(userId: Long): List<UserOAuth>
    fun findByEmail(email: String): UserOAuth?
}
