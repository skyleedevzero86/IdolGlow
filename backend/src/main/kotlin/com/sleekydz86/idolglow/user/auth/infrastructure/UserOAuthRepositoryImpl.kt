package com.sleekydz86.idolglow.user.auth.infrastructure

import com.sleekydz86.idolglow.user.auth.domain.UserOAuth
import com.sleekydz86.idolglow.user.auth.domain.UserOAuthRepository
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import org.springframework.stereotype.Repository

@Repository
class UserOAuthRepositoryImpl(
    private val userOAuthJpaRepository: UserOAuthJpaRepository
) : UserOAuthRepository {

    override fun save(userOAuth: UserOAuth): UserOAuth =
        userOAuthJpaRepository.save(userOAuth)

    override fun findByProviderAndProviderId(
        provider: AuthProvider,
        providerId: String
    ): UserOAuth? = userOAuthJpaRepository.findByProviderAndProviderId(provider, providerId)

    override fun findAllByUserId(userId: Long): List<UserOAuth> =
        userOAuthJpaRepository.findAllByUserId(userId)

    override fun findByEmail(email: String): UserOAuth? =
        userOAuthJpaRepository.findByEmail(email)
}
