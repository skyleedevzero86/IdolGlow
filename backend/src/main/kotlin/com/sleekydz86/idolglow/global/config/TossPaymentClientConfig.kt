package com.sleekydz86.idolglow.global.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class TossPaymentClientConfig(
    private val tossPaymentProperties: TossPaymentProperties,
    @param:Value("\${payment.toss.billing.read-timeout-ms:60000}")
    private val billingReadTimeoutMs: Int,
) {
    @Bean(name = ["tossRestClient"])
    fun tossRestClient(): RestClient = client(sharedRequestFactory())

    @Bean(name = ["tossBillingRestClient"])
    fun tossBillingRestClient(): RestClient = client(billingRequestFactory())

    fun sharedRequestFactory(): TossTimeoutRequestFactory =
        requestFactory(tossPaymentProperties.connectTimeoutMs, tossPaymentProperties.readTimeoutMs)

    fun billingRequestFactory(): TossTimeoutRequestFactory =
        requestFactory(tossPaymentProperties.connectTimeoutMs, billingReadTimeout())

    private fun billingReadTimeout(): Int = billingReadTimeoutMs.coerceAtLeast(MIN_BILLING_READ_TIMEOUT_MS)

    private fun client(factory: TossTimeoutRequestFactory): RestClient =
        RestClient
            .builder()
            .baseUrl(tossPaymentProperties.baseUrl.trim().ifBlank { "https://api.tosspayments.com" })
            .requestFactory(factory)
            .build()

    class TossTimeoutRequestFactory(
        val connectTimeoutMs: Int,
        val readTimeoutMs: Int,
    ) : SimpleClientHttpRequestFactory() {
        init {
            setConnectTimeout(connectTimeoutMs)
            setReadTimeout(readTimeoutMs)
        }
    }

    companion object {
        const val MIN_BILLING_READ_TIMEOUT_MS = 60_000

        fun requestFactory(
            connectTimeoutMs: Int,
            readTimeoutMs: Int,
        ): TossTimeoutRequestFactory = TossTimeoutRequestFactory(connectTimeoutMs, readTimeoutMs)
    }
}
