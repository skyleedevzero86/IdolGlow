package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailSubscriptionPort
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class UnsubscribeTokenService(
    private val productRecommendationMailSubscriptionPort: ProductRecommendationMailSubscriptionPort,
    @param:Value("\${app.mail.recommendation.unsubscribe-secret}")
    secret: String,
) {
    private val key: SecretKeySpec

    init {
        check(secret.isNotBlank()) { "app.mail.recommendation.unsubscribe-secret 가 비어 있습니다." }
        key = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), ALGORITHM)
    }

    fun issue(subscription: ProductRecommendationMailSubscription): String =
        issue(subscription.id, subscription.tokenVersion)

    fun issue(
        subscriptionId: Long,
        tokenVersion: Int,
    ): String {
        require(subscriptionId > 0) { "구독 ID는 0보다 커야 합니다. subscriptionId=$subscriptionId" }
        val payload =
            subscriptionId.toString().toByteArray(StandardCharsets.US_ASCII) +
                SEPARATOR +
                sign(subscriptionId, tokenVersion)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
    }

    fun verify(token: String): ProductRecommendationMailSubscription? {
        val parsed = parse(token) ?: return null
        return productRecommendationMailSubscriptionPort
            .findById(parsed.subscriptionId)
            ?.takeIf { MessageDigest.isEqual(sign(it.id, it.tokenVersion), parsed.signature) }
    }

    private fun parse(token: String): ParsedToken? {
        val bytes =
            token
                .trim()
                .takeIf { it.isNotEmpty() && it.length <= MAX_TOKEN_LENGTH }
                ?.let { runCatching { Base64.getUrlDecoder().decode(it) }.getOrNull() }
                ?: return null
        val separatorIndex = bytes.indexOf(SEPARATOR)
        val idText =
            if (separatorIndex > 0) String(bytes, 0, separatorIndex, StandardCharsets.US_ASCII) else ""
        return idText
            .takeIf { text -> text.all { it in '0'..'9' } }
            ?.toLongOrNull()
            ?.takeIf { it > 0 }
            ?.let { ParsedToken(it, bytes.copyOfRange(separatorIndex + 1, bytes.size)) }
    }

    private fun sign(
        subscriptionId: Long,
        tokenVersion: Int,
    ): ByteArray =
        Mac
            .getInstance(ALGORITHM)
            .apply { init(key) }
            .doFinal("$subscriptionId:$tokenVersion".toByteArray(StandardCharsets.UTF_8))

    private class ParsedToken(
        val subscriptionId: Long,
        val signature: ByteArray,
    )

    companion object {
        private const val ALGORITHM = "HmacSHA256"
        private const val MAX_TOKEN_LENGTH = 128
        private val SEPARATOR: Byte = '.'.code.toByte()
    }
}
