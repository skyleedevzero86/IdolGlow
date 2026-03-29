package com.sleekydz86.idolglow.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class TossPaymentClientConfig(
    private val tossPaymentProperties: TossPaymentProperties,
) {

    @Bean(name = ["tossRestClient"])
    fun tossRestClient(): RestClient {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(tossPaymentProperties.connectTimeoutMs)
        factory.setReadTimeout(tossPaymentProperties.readTimeoutMs)
        return RestClient.builder()
            .baseUrl(tossPaymentProperties.baseUrl.trim().ifBlank { "https://api.tosspayments.com" })
            .requestFactory(factory)
            .build()
    }
}
