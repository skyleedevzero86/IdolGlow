package com.sleekydz86.idolglow.partner.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object PartnerKey {
    private const val PREFIX: String = "pk_"
    private const val RANDOM_BYTES: Int = 32
    private val random = SecureRandom()

    fun generate(): String {
        val bytes = ByteArray(RANDOM_BYTES).also(random::nextBytes)
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(rawKey: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(rawKey.trim().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
