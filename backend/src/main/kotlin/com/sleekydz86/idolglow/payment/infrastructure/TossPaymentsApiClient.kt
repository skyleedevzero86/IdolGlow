package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.util.UriUtils
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import java.util.Base64

@Component
class TossPaymentsApiClient(
    private val props: TossPaymentProperties,
    @param:Qualifier("tossRestClient") private val restClient: RestClient,
    @param:Qualifier("tossBillingRestClient") private val billingRestClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: MeterRegistry = SimpleMeterRegistry(),
) {
    fun confirm(
        paymentKey: String,
        orderId: String,
        amount: Long,
        idempotencyKey: String?,
    ): TossApiResponse {
        val path = "/v1/payments/confirm"
        val body =
            objectMapper.createObjectNode().apply {
                put("paymentKey", paymentKey)
                put("orderId", orderId)
                put("amount", amount)
            }
        return post("confirm", path, objectMapper.writeValueAsString(body), idempotencyKey)
    }

    fun getPayment(paymentKey: String): TossApiResponse {
        val encoded = UriUtils.encodePathSegment(paymentKey, StandardCharsets.UTF_8)
        val path = "/v1/payments/$encoded"
        return get("getPayment", path)
    }

    fun getPaymentByOrderId(orderId: String): TossApiResponse {
        val encoded = UriUtils.encodePathSegment(orderId, StandardCharsets.UTF_8)
        return get("getPaymentByOrderId", "/v1/payments/orders/$encoded")
    }

    fun cancel(
        paymentKey: String,
        cancelReason: String,
        cancelAmount: Long?,
        idempotencyKey: String?,
    ): TossApiResponse {
        val encoded = UriUtils.encodePathSegment(paymentKey, StandardCharsets.UTF_8)
        val path = "/v1/payments/$encoded/cancel"
        val body =
            objectMapper.createObjectNode().apply {
                put("cancelReason", cancelReason)
                if (cancelAmount != null) {
                    put("cancelAmount", cancelAmount)
                }
            }
        return post("cancel", path, objectMapper.writeValueAsString(body), idempotencyKey)
    }

    fun issueBillingKey(
        authKey: String,
        customerKey: String,
    ): TossApiResponse {
        val body =
            objectMapper.createObjectNode().apply {
                put("authKey", authKey)
                put("customerKey", customerKey)
            }
        return post("issueBillingKey", "/v1/billing/authorizations/issue", objectMapper.writeValueAsString(body), null)
    }

    fun chargeBilling(
        billingKey: String,
        customerKey: String,
        amount: Long,
        orderId: String,
        orderName: String,
        customerEmail: String?,
        customerName: String?,
        idempotencyKey: String?,
    ): TossApiResponse {
        val encoded = UriUtils.encodePathSegment(billingKey, StandardCharsets.UTF_8)
        val body =
            objectMapper.createObjectNode().apply {
                put("customerKey", customerKey)
                put("amount", amount)
                put("orderId", orderId)
                put("orderName", orderName)
                if (!customerEmail.isNullOrBlank()) {
                    put("customerEmail", customerEmail)
                }
                if (!customerName.isNullOrBlank()) {
                    put("customerName", customerName)
                }
                put("taxFreeAmount", 0)
            }
        return post(
            api = "chargeBilling",
            path = "/v1/billing/$encoded",
            jsonBody = objectMapper.writeValueAsString(body),
            idempotencyKey = idempotencyKey,
            client = billingRestClient,
        )
    }

    fun deleteBillingKey(billingKey: String): TossApiResponse {
        val encoded = UriUtils.encodePathSegment(billingKey, StandardCharsets.UTF_8)
        val auth = basicAuth()
        return exchange(
            "deleteBillingKey",
            restClient
                .delete()
                .uri("/v1/billing/$encoded")
                .headers { h ->
                    h.add(HttpHeaders.AUTHORIZATION, "Basic $auth")
                },
        )
    }

    private fun get(
        api: String,
        path: String,
    ): TossApiResponse {
        val auth = basicAuth()
        return exchange(
            api,
            restClient
                .get()
                .uri(path)
                .headers { h ->
                    h.add(HttpHeaders.AUTHORIZATION, "Basic $auth")
                },
        )
    }

    private fun post(
        api: String,
        path: String,
        jsonBody: String,
        idempotencyKey: String?,
        client: RestClient = restClient,
    ): TossApiResponse {
        val auth = basicAuth()
        return exchange(
            api,
            client
                .post()
                .uri(path)
                .headers { h ->
                    h.add(HttpHeaders.AUTHORIZATION, "Basic $auth")
                    h.contentType = MediaType.APPLICATION_JSON
                    if (!idempotencyKey.isNullOrBlank()) {
                        h.add("Idempotency-Key", idempotencyKey)
                    }
                }.body(jsonBody),
        )
    }

    private fun exchange(
        api: String,
        spec: RestClient.RequestHeadersSpec<*>,
    ): TossApiResponse {
        val response =
            try {
                val entity = spec.retrieve().toEntity(String::class.java)
                val text = entity.body ?: ""
                val node = runCatching { objectMapper.readTree(text) }.getOrNull()
                TossApiResponse(entity.statusCode.value(), node, text, null)
            } catch (e: RestClientResponseException) {
                val text = e.responseBodyAsString
                val node = runCatching { objectMapper.readTree(text) }.getOrNull()
                TossApiResponse(e.statusCode.value(), node, text, e)
            } catch (e: Exception) {
                TossApiResponse(0, null, e.message, e)
            }
        recordTossApiCall(meterRegistry, api, response)
        return response
    }

    private fun basicAuth(): String {
        val secret = props.secretKey.trim()
        require(secret.isNotEmpty()) { "payment.toss.secret-key 가 설정되어 있어야 합니다." }
        return Base64.getEncoder().encodeToString("$secret:".toByteArray(StandardCharsets.UTF_8))
    }
}
