package com.sleekydz86.idolglow.user.auth.application

import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse
import org.springframework.stereotype.Service
import java.security.AuthProvider

@Service
class LoginFacade(
    strategies: List<LoginStrategy>
) {
    private val map: Map<AuthProvider, LoginStrategy> =
        strategies
            .groupBy { it.provider }
            .also { grouped ->
                val duplicates = grouped.filterValues { it.size > 1 }.keys
                require(duplicates.isEmpty()){ "Duplicate LoginStrategy for provider: $duplicates" }
            }
            .mapValues { (_, list) -> list.single() }

    fun login(provider: AuthProvider, request: LoginRequest): TokenResponse =
        map[provider]?.login(request)
            ?: throw IllegalArgumentException("Unsupported provider: $provider")
}
