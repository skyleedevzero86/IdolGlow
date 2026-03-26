package com.sleekydz86.idolglow.user.auth.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.AuthProvider

@Service
class TestSignupService(
    private val userRepository: UserRepository,
    private val userOAuthRepository: UserOAuthRepository
) {

    @Transactional
    fun testSignup(request: TestSignupRequest): TestSignupResponse {
        val result = signupOrGetTestUser(request.seed)
        return TestSignupResponse(
            userId = result.userId,
            email = result.email
        )
    }

    private fun signupOrGetTestUser(seed: String): TestUser {
        val email = "test+${seed}@google.com"
        val provider = AuthProvider.TEST
        val providerId = "test-${seed}"

        val userByEmail: User? = userRepository.findByEmail(email = email)
        if (userByEmail != null) {
            ensureOAuthMapping(userByEmail.id, provider, providerId, email)
            return TestUser(userId = userByEmail.id, email = userByEmail.email)
        }

        val saved: User = userRepository.save(User.of(email = email))
        ensureOAuthMapping(saved.id, provider, providerId, email)

        return TestUser(userId = saved.id, email = saved.email)
    }

    private fun ensureOAuthMapping(userId: Long, provider: AuthProvider, providerId: String, email: String) {
        val existing = userOAuthRepository.findByProviderAndProviderId(provider, providerId)
        if (existing != null) return

        val existingByEmail = userOAuthRepository.findByEmail(email)
        if (existingByEmail != null) return

        userOAuthRepository.save(
            UserOAuth.of(
                userId = userId,
                provider = provider,
                providerId = providerId,
                email = email
            )
        )
    }
}
