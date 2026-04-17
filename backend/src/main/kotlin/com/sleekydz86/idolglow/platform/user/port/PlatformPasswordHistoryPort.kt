package com.sleekydz86.idolglow.platform.user.port

interface PlatformPasswordHistoryPort {
    fun findRecentEncodedPasswords(userId: Long, limit: Int): List<String>
    fun append(userId: Long, encodedPassword: String)
}
