package com.sleekydz86.idolglow.user.auth.application

data class LoginRequest(
    val email: String? = null,
    val attributes: Map<String, Any>? = null,
)
