package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class GemmaClientConfig(
    private val gemmaProperties: GemmaProperties,
) {

    @Bean(name = ["gemmaRestClient"])
    fun gemmaRestClient(): RestClient {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(gemmaProperties.connectTimeoutMs)
        factory.setReadTimeout(gemmaProperties.readTimeoutMs)

        return RestClient.builder()
            .baseUrl(gemmaProperties.baseUrl.trim().ifBlank { "http://localhost:8080/v1" })
            .requestFactory(factory)
            .build()
    }
}
