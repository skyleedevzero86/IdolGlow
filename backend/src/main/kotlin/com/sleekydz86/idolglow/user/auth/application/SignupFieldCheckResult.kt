package com.sleekydz86.idolglow.user.auth.application

data class SignupFieldCheckResult(
    val available: Boolean,
    val code: String? = null,
)
