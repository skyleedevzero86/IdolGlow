package com.sleekydz86.idolglow.user.auth.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val userOAuthRepository: UserOAuthRepository,
    private val jwtProvider: JwtProvider,
) {

    @Transactional
    fun login(provider: AuthProvider, providerId: String, email: String): TokenResponse {
        // 소셜계정 등록 여부 검사
        val oauthUser = userOAuthRepository.findByProviderAndProviderId(provider, providerId)

        val userId: Long = oauthUser?.userId
            ?: run {

                val user: User = userRepository.findByEmail(email)

                    ?: userRepository.save(User.of(email = email))

                UserOAuth.of(
                    userId = user.id,
                    provider = provider,
                    providerId = providerId,
                    email = email
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
