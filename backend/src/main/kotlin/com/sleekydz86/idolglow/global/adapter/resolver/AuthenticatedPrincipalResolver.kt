package com.sleekydz86.idolglow.global.adapter.resolver

import com.sleekydz86.idolglow.global.adapter.security.AuthChannel
import com.sleekydz86.idolglow.global.adapter.security.AuthenticatedPrincipal
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.auth.AuthExceptionType
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuthenticatedPrincipalResolver(
    private val userRepository: UserRepository,
) {
    fun resolveOrNull(): AuthenticatedPrincipal? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        if (!authentication.isAuthenticated || authentication.name == "anonymousUser") {
            return null
        }

        val roles =
            authentication.authorities
                .mapNotNull { it.authority?.removePrefix("ROLE_") }
                .toSet()
        val name = authentication.name?.trim().orEmpty()
        if (name.isEmpty()) {
            return null
        }

        name.toLongOrNull()?.let { userId ->
            return AuthenticatedPrincipal(
                channel = AuthChannel.GLOBAL,
                accountId = userId,
                userId = userId,
                platformId = null,
                roles = roles,
            )
        }

        if (name.contains('@')) {
            val user =
                userRepository.findByEmail(name)
                    ?: return null
            return AuthenticatedPrincipal(
                channel = AuthChannel.PLATFORM,
                accountId = user.id,
                userId = user.id,
                platformId = null,
                roles = roles,
                email = name,
            )
        }

        return null
    }

    fun resolveRequired(): AuthenticatedPrincipal =
        resolveOrNull() ?: throw CustomException(AuthExceptionType.UNAUTHENTICATED)

    fun requireUserId(): Long {
        val principal = resolveRequired()
        return principal.userId ?: throw CustomException(AuthExceptionType.INVALID_AUTH_PRINCIPAL)
    }
}
