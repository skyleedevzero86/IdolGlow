package com.sleekydz86.idolglow.payment.infrastructure

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object BillingKeyCrypto {
    private const val PREFIX = "v1:"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    @Volatile
    private var secretKey: SecretKeySpec? = null

    fun configure(secret: String) {
        require(secret.isNotBlank()) { "payment.toss.billing.crypto.secret 가 비어 있습니다." }
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(StandardCharsets.UTF_8))
        secretKey = SecretKeySpec(keyBytes, "AES")
    }

    fun isConfigured(): Boolean = secretKey != null

    fun sha256Hex(plain: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(plain.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun encrypt(plain: String): String {
        val key = secretKey ?: return plain
        if (plain.startsWith(PREFIX)) {
            return plain
        }
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8))
        val packed = ByteBuffer.allocate(iv.size + encrypted.size).put(iv).put(encrypted).array()
        return PREFIX + Base64.getEncoder().encodeToString(packed)
    }

    fun decrypt(stored: String): String {
        val key = secretKey ?: return stored
        if (!stored.startsWith(PREFIX)) {
            return stored
        }
        val packed = Base64.getDecoder().decode(stored.removePrefix(PREFIX))
        require(packed.size > IV_BYTES) { "빌링키 암호문 형식이 올바르지 않습니다. length=${packed.size}" }
        val iv = packed.copyOfRange(0, IV_BYTES)
        val encrypted = packed.copyOfRange(IV_BYTES, packed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }
}
