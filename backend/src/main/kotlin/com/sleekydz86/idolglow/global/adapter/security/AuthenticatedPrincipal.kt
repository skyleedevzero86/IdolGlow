package com.sleekydz86.idolglow.global.adapter.security

data class AuthenticatedPrincipal(
    val channel: AuthChannel,
    val accountId: Long,
    val userId: Long?,
    val platformId: Long?,
    val roles: Set<String>,
    val email: String? = null,
) {
    fun hasRole(role: String): Boolean {
        val normalized = role.removePrefix("ROLE_")
        return roles.any { it.removePrefix("ROLE_") == normalized }
    }

    fun requireUserId(): Long = userId ?: error("인증 주체에 userId가 없습니다. channel=$channel")
}
