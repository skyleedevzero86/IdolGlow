package com.sleekydz86.idolglow.global.adapter.resolver

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.auth.AuthExceptionType
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuthenticatedUserIdResolver(
    private val userRepository: UserRepository,
) {

    fun resolveRequired(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw CustomException(AuthExceptionType.UNAUTHENTICATED)

        if (!authentication.isAuthenticated || authentication.name == "anonymousUser") {
            throw CustomException(AuthExceptionType.UNAUTHENTICATED)
        }

        val name = authentication.name
        name.toLongOrNull()?.let { return it }

        if (name.contains('@')) {
            val user = userRepository.findByEmail(name)
                ?: throw CustomException(AuthExceptionType.INVALID_AUTH_PRINCIPAL)
            return user.id
        }

        throw CustomException(AuthExceptionType.INVALID_AUTH_PRINCIPAL)
    }
}
