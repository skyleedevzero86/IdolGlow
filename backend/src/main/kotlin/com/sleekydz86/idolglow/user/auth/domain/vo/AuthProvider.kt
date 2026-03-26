package com.sleekydz86.idolglow.user.auth.domain.vo


enum class AuthProvider(val registrationId: String) {
    GOOGLE("google"),
    TEST("test");

    companion object {
        private val allowedRegistrationIds: Set<String> =
            entries.map { it.registrationId }.toSet()

        fun isAllowedRegistrationId(registrationId: String): Boolean =
            registrationId in allowedRegistrationIds

        fun fromRegistrationId(registrationId: String): AuthProvider =
            entries.firstOrNull { it.registrationId.equals(registrationId, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported provider: $registrationId")
    }
}