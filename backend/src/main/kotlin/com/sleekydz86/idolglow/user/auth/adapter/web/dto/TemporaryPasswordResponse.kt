package com.sleekydz86.idolglow.user.auth.ui.dto

import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse

data class TemporaryPasswordResponse(
    val sent: Boolean,
    val message: String,
)
