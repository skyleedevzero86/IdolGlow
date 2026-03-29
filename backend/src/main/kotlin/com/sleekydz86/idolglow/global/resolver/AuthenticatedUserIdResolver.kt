package com.sleekydz86.idolglow.global.resolver

import com.sleekydz86.idolglow.global.exceptions.CustomException
import com.sleekydz86.idolglow.global.exceptions.auth.AuthExceptionType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuthenticatedUserIdResolver {

    fun resolveRequired(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw CustomException(AuthExceptionType.UNAUTHENTICATED)

        if (!authentication.isAuthenticated || authentication.name == "anonymousUser") {
            throw CustomException(AuthExceptionType.UNAUTHENTICATED)
        }

        return authentication.name.toLongOrNull()
            ?: throw CustomException(AuthExceptionType.INVALID_AUTH_PRINCIPAL)
    }
}
