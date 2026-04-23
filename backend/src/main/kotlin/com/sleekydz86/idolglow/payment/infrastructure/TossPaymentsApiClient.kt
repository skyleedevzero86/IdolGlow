package com.sleekydz86.idolglow.payment.infrastructure

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import com.sleekydz86.idolglow.global.infrastructure.config.TossPaymentProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriUtils
import java.nio.charset.StandardCharsets
import java.util.Base64

@Component
class TossPaymentsApiClient(
    private val props: TossPaymentProperties,
    @param:Qualifier("tossRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
) {

    fun confirm(
        paymentKey: String,
        orderId: String,
        amount: Long,
        idempotencyKey: String?,
    ): TossApiResponse {
        val path = "/v1/payments/confirm"
        val body = objectMapper.createObjectNode().apply {
            put("paymentKey", paymentKey)
            put("orderId", orderId)
            put("amount", amount)
        }
        return post(path, objectMapper.writeValueAsString(body), idempotencyKey)
    }

    fun getPayment(paymentKey: String): TossApiResponse {
        val encoded = UriUtils.encodePathSegment(paymentKey, StandardCharsets.UTF_8)
        val path = "/v1/payments/$encoded"
        return get(path)
    }

    fun cancel(
        paymentKey: String,
        cancelReason: String,
        cancelAmount: Long?,
        idempotencyKey: String?,
    ): TossApiResponse {
        val encoded = UriUtils.encodePathSegment(paymentKey, StandardCharsets.UTF_8)
        val path = "/v1/payments/$encoded/cancel"
        val body = objectMapper.createObjectNode().apply {
            put("cancelReason", cancelReason)
            if (cancelAmount != null) {
                put("cancelAmount", cancelAmount)
            }
        }
        return post(path, objectMapper.writeValueAsString(body), idempotencyKey)
    }

    private fun get(path: String): TossApiResponse {
        val secret = props.secretKey.trim()
        require(secret.isNotEmpty()) { "payment.toss.secret-key 가 설정되어 있어야 합니다." }
        val auth = Base64.getEncoder().encodeToString("$secret:".toByteArray(StandardCharsets.UTF_8))

        return try {
            val entity = restClient.get()
                .uri(path)
                .headers { h ->
                    h.add(HttpHeaders.AUTHORIZATION, "Basic $auth")
                }
                .retrieve()
                .toEntity(String::class.java)
            val text = entity.body ?: ""
            val node = runCatching { objectMapper.readTree(text) }.getOrNull()
            TossApiResponse(entity.statusCode.value(), node, text, null)
        } catch (e: Exception) {
            TossApiResponse(0, null, e.message, e)
        }
    }

    private fun post(path: String, jsonBody: String, idempotencyKey: String?): TossApiResponse {
        val secret = props.secretKey.trim()
        require(secret.isNotEmpty()) { "payment.toss.secret-key 가 설정되어 있어야 합니다." }
        val auth = Base64.getEncoder().encodeToString("$secret:".toByteArray(StandardCharsets.UTF_8))

        return try {
            val entity = restClient.post()
                .uri(path)
                .headers { h ->
                    h.add(HttpHeaders.AUTHORIZATION, "Basic $auth")
                    h.contentType = MediaType.APPLICATION_JSON
                    if (!idempotencyKey.isNullOrBlank()) {
                        h.add("Idempotency-Key", idempotencyKey)
                    }
                }
                .body(jsonBody)
                .retrieve()
                .toEntity(String::class.java)
            val text = entity.body ?: ""
            val node = runCatching { objectMapper.readTree(text) }.getOrNull()
            TossApiResponse(entity.statusCode.value(), node, text, null)
        } catch (e: Exception) {
            TossApiResponse(0, null, e.message, e)
        }
    }
}

data class TossApiResponse(
    val httpStatus: Int,
    val json: JsonNode?,
    val rawBody: String?,
    val error: Throwable? = null,
) {
    val isSuccess2xx: Boolean get() = httpStatus in 200..299
}
