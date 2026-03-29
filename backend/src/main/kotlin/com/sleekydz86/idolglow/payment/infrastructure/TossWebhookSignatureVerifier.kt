package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class TossWebhookSignatureVerifier(
    private val props: TossPaymentProperties,
) {

    fun verify(rawBody: String, signatureHeader: String?, transmissionTime: String?): Boolean {
        val secret = effectiveSecret()
        if (secret.isBlank() || signatureHeader.isNullOrBlank() || transmissionTime.isNullOrBlank()) {
            return false
        }
        val message = "$rawBody:$transmissionTime"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val expected = mac.doFinal(message.toByteArray(StandardCharsets.UTF_8))
        val expectedB64 = Base64.getEncoder().encodeToString(expected)

        val candidates = signatureHeader.split(',')
            .map { it.trim() }
            .filter { it.startsWith("v1:") }
            .map { it.removePrefix("v1:").trim() }

        return candidates.any { candidate ->
            try {
                val decoded = Base64.getDecoder().decode(candidate)
                MessageDigest.isEqual(expected, decoded) ||
                    candidate == expectedB64
            } catch (_: IllegalArgumentException) {
                candidate == expectedB64
            }
        }
    }

    private fun effectiveSecret(): String =
        props.webhookSecret.trim().ifBlank { props.secretKey.trim() }
}
