package com.sleekydz86.idolglow.user.auth.application

import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse
import com.sleekydz86.idolglow.user.auth.application.strategy.LoginStrategy
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import org.springframework.stereotype.Service

@Service
class LoginFacade(
    strategies: List<LoginStrategy>
) {
    private val map: Map<AuthProvider, LoginStrategy> =
        strategies
            .groupBy { it.provider }
            .also { grouped ->
                val duplicates = grouped.filterValues { it.size > 1 }.keys
                require(duplicates.isEmpty()) { "로그인 전략이 중복 등록되었습니다: $duplicates" }
            }
            .mapValues { (_, list) -> list.single() }

    fun login(provider: AuthProvider, request: LoginRequest): TokenResponse =
        map[provider]?.login(request)
            ?: throw IllegalArgumentException("지원하지 않는 로그인 제공자입니다: $provider")
}
