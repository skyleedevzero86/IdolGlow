package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.global.config.TossPaymentClientConfig
import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.Collections

@DisplayName("빌링 승인 읽기 타임아웃")
class TossBillingReadTimeoutTest {
    @DisplayName("빌링 승인 읽기 타임아웃 기본값은 60초이고 공용은 30초다")
    @Test
    fun billingClientUses60sReadTimeout() {
        // given
        val config = config(billingReadTimeoutMs = 60_000)

        // when
        val shared = config.sharedRequestFactory()
        val billing = config.billingRequestFactory()

        // then
        assertEquals(30_000, shared.readTimeoutMs)
        assertEquals(60_000, billing.readTimeoutMs)
        assertEquals(10_000, shared.connectTimeoutMs)
        assertEquals(10_000, billing.connectTimeoutMs)
    }

    @DisplayName("빌링 승인 읽기 타임아웃은 설정을 따르고 60초보다 짧으면 60초로 올린다")
    @Test
    fun billingClientUsesConfiguredReadTimeout() {
        // given
        val longer = config(billingReadTimeoutMs = 90_000)
        val shorter = config(billingReadTimeoutMs = 15_000)

        // when
        val custom = longer.billingRequestFactory()
        val floored = shorter.billingRequestFactory()

        // then
        assertEquals(90_000, custom.readTimeoutMs)
        assertEquals(10_000, custom.connectTimeoutMs)
        assertEquals(30_000, longer.sharedRequestFactory().readTimeoutMs)
        assertEquals(60_000, floored.readTimeoutMs)
    }

    @DisplayName("자동결제 승인만 빌링 전용 클라이언트를 사용한다")
    @Test
    fun chargeBillingUsesBillingRestClient() {
        // given
        val shared = RecordingServer()
        val billing = RecordingServer()
        val api = apiClient(shared.restClient(), billing.restClient())

        // when
        shared.use {
            billing.use {
                api.chargeBilling(
                    billingKey = "bk-1",
                    customerKey = "customer-1",
                    amount = 4_900,
                    orderId = "order-1",
                    orderName = "IdolGlow",
                    customerEmail = null,
                    customerName = null,
                    idempotencyKey = "billing-charge-order-1",
                )
            }
        }

        // then
        assertTrue(shared.paths.isEmpty())
        assertEquals(listOf("/v1/billing/bk-1"), billing.paths)
    }

    @DisplayName("주문번호 조회는 공용 클라이언트를 사용한다")
    @Test
    fun paymentLookupStillUsesDefaultRestClient() {
        // given
        val shared = RecordingServer()
        val billing = RecordingServer()
        val api = apiClient(shared.restClient(), billing.restClient())

        // when
        shared.use {
            billing.use {
                api.getPaymentByOrderId("order-1")
            }
        }

        // then
        assertEquals(listOf("/v1/payments/orders/order-1"), shared.paths)
        assertTrue(billing.paths.isEmpty())
    }

    private fun config(billingReadTimeoutMs: Int): TossPaymentClientConfig =
        TossPaymentClientConfig(
            tossPaymentProperties =
                TossPaymentProperties(
                    connectTimeoutMs = 10_000,
                    readTimeoutMs = 30_000,
                ),
            billingReadTimeoutMs = billingReadTimeoutMs,
        )

    private fun apiClient(
        shared: RestClient,
        billing: RestClient,
    ): TossPaymentsApiClient =
        TossPaymentsApiClient(
            props = TossPaymentProperties(secretKey = "test-secret"),
            restClient = shared,
            billingRestClient = billing,
            objectMapper = ObjectMapper(),
        )

    private class RecordingServer : AutoCloseable {
        val paths: MutableList<String> = Collections.synchronizedList(mutableListOf())
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)

        init {
            server.createContext("/") { exchange ->
                paths.add(exchange.requestURI.path)
                exchange.requestBody.use { it.readBytes() }
                val body = "{}".toByteArray(StandardCharsets.UTF_8)
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            server.start()
        }

        fun restClient(): RestClient = RestClient.builder().baseUrl("http://127.0.0.1:${server.address.port}").build()

        override fun close() {
            server.stop(0)
        }
    }
}
