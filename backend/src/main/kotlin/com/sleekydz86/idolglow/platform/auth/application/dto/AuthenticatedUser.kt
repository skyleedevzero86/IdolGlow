package com.sleekydz86.idolglow.platform.auth.application.dto

data class AuthenticatedUser(
    val email: String,
    val role: String,
) {
    companion object {
        fun builder(): Builder = Builder()

        class Builder {
            private var email: String? = null
            private var role: String? = null

            fun email(email: String) = apply { this.email = email }
            fun role(role: String) = apply { this.role = role }

            fun build(): AuthenticatedUser =
                AuthenticatedUser(
                    email = email ?: error("이메일이 필요합니다."),
                    role = role ?: error("역할이 필요합니다."),
                )
        }
    }
}
