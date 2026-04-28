package com.sleekydz86.idolglow.global.adapter.security

import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.security.Key

object JwtSigningKeyFactory {

    fun create(secret: String): Key {
        val normalizedSecret = secret.trim()
        val keyBytes = decodeBase64(normalizedSecret) ?: normalizedSecret.toByteArray(StandardCharsets.UTF_8)
        val finalBytes = if (keyBytes.size < MINIMUM_KEY_BYTES) {
            ByteArray(MINIMUM_KEY_BYTES).also { padded ->
                System.arraycopy(keyBytes, 0, padded, 0, keyBytes.size)
            }
        } else {
            keyBytes
        }

        return Keys.hmacShaKeyFor(finalBytes)
    }

    private fun decodeBase64(secret: String): ByteArray? =
        try {
            Decoders.BASE64.decode(secret)
        } catch (_: IllegalArgumentException) {
            null
        }

    private const val MINIMUM_KEY_BYTES = 32
}
