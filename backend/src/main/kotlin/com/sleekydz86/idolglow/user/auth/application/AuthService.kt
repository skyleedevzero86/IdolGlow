package com.sleekydz86.idolglow.user.auth.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.auth.AuthExceptionType
import com.sleekydz86.idolglow.global.adapter.security.JwtProvider
import com.sleekydz86.idolglow.global.adapter.security.JwtTokenType
import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse
import com.sleekydz86.idolglow.user.auth.domain.UserOAuth
import com.sleekydz86.idolglow.user.auth.domain.UserOAuthRepository
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val userOAuthRepository: UserOAuthRepository,
    private val jwtProvider: JwtProvider,
) {

    @Transactional
    fun login(
        provider: AuthProvider,
        providerId: String,
        email: String,
        name: String? = null,
        picture: String? = null,
    ): TokenResponse {
        val oauthUser = userOAuthRepository.findByProviderAndProviderId(provider, providerId)
        oauthUser?.updateProfile(name = name, picture = picture)

        val userId: Long = oauthUser?.userId
            ?: run {

                val user: User = userRepository.findByEmail(email)
                    ?: userRepository.saveAndFlush(User.of(email = email))

                UserOAuth.of(
                    userId = user.id,
                    provider = provider,
                    providerId = providerId,
                    email = email,
                    profileName = name,
                    profileImageUrl = picture,
                ).let { userOAuthRepository.save(it) }
                user.id
            }

        val user = userRepository.findById(userId)
            ?: throw CustomException(AuthExceptionType.USER_NOT_FOUND)

        user.updateLastLoginTime()

        return jwtProvider.generateToken(user.id, user.role)
    }

    @Transactional
    fun reissue(refreshToken: String): TokenResponse {
        if (refreshToken.isBlank()) {
            throw CustomException(AuthExceptionType.UNAUTHENTICATED)
        }

        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw CustomException(AuthExceptionType.INVALID_REFRESH_TOKEN)
        }

        if (jwtProvider.getTokenType(refreshToken) != JwtTokenType.REFRESH) {
            throw CustomException(AuthExceptionType.INVALID_TOKEN_TYPE)
        }

        val userId = jwtProvider.getSubjectAsUserId(refreshToken)
        val user = userRepository.findById(userId)
            ?: throw CustomException(AuthExceptionType.USER_NOT_FOUND)

        return jwtProvider.generateToken(user.id, user.role)
    }
}
