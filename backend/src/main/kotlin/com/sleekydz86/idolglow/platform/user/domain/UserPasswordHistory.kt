package com.sleekydz86.idolglow.platform.user.domain

import java.time.LocalDateTime

data class UserPasswordHistory(
    val id: Long? = null,
    val userId: Long,
    val passwordHash: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
