package com.sleekydz86.idolglow.global.adapter.resolver

import org.springframework.stereotype.Component

@Component
class AuthenticatedUserIdResolver(
    private val authenticatedPrincipalResolver: AuthenticatedPrincipalResolver,
) {
    fun resolveOrNull(): Long? = authenticatedPrincipalResolver.resolveOrNull()?.userId

    fun resolveRequired(): Long = authenticatedPrincipalResolver.requireUserId()
}
