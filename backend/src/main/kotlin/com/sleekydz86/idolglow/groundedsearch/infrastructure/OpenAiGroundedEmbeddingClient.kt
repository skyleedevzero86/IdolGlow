package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.application.port.EmbeddingPort
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Component
@Profile("!test")
class OpenAiGroundedEmbeddingClient(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val environment: Environment,
) : EmbeddingPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun modelName(): String = required(MODEL_KEY)

    override fun embed(texts: List<String>): List<FloatArray> {
        if (texts.isEmpty()) {
            return emptyList()
        }
        return parse(post(texts))
    }

    private fun post(texts: List<String>): String {
        if (!ready()) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "근거 검색 임베딩 설정이 없습니다. 모델과 키를 확인해 주세요.")
        }
        return runCatching { call(texts) }.getOrElse { error -> fail(error) }
    }

    private fun call(texts: List<String>): String =
        webClient
            .post()
            .uri("${baseUrl()}/v1/embeddings")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${apiKey()}")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .bodyValue(GroundedEmbeddingRequest(modelName(), texts))
            .retrieve()
            .bodyToMono(String::class.java)
            .timeout(Duration.ofSeconds(timeoutSeconds()))
            .block()
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "근거 검색 임베딩 응답이 비어 있습니다.")

    private fun parse(body: String): List<FloatArray> {
        val rows =
            runCatching {
                objectMapper.readValue(body, GroundedEmbeddingResponse::class.java).data.sortedBy { it.index }
            }.getOrElse { error ->
                log.warn("근거 검색 임베딩 응답 해석 실패 type={}", error.javaClass.simpleName, error)
                throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "근거 검색 임베딩 응답을 해석하지 못했습니다.", error)
            }
        if (rows.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "근거 검색 임베딩 결과가 비어 있습니다.")
        }
        return rows.map { row -> FloatArray(row.embedding.size) { index -> row.embedding[index].toFloat() } }
    }

    private fun fail(error: Throwable): Nothing {
        log.warn("근거 검색 임베딩 호출 실패 type={}", error.javaClass.simpleName, error)
        if (error is ResponseStatusException) {
            throw error
        }
        throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "근거 검색 임베딩 호출에 실패했습니다.", error)
    }

    private fun ready(): Boolean =
        environment.getProperty(ENABLED_KEY, Boolean::class.java, false) && apiKey().isNotBlank()

    private fun apiKey(): String = environment.getProperty(API_KEY).orEmpty().trim()

    private fun baseUrl(): String = required(BASE_URL).trimEnd('/')

    private fun timeoutSeconds(): Long {
        val raw = environment.getProperty(TIMEOUT_KEY)?.toLongOrNull()
        if (raw == null || raw <= 0L) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "근거 검색 임베딩 제한 시간이 없습니다.")
        }
        return raw
    }

    private fun required(key: String): String {
        val value = environment.getProperty(key)?.trim().orEmpty()
        if (value.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "근거 검색 임베딩 설정이 없습니다. key=$key")
        }
        return value
    }

    private companion object {
        private const val MODEL_KEY = "app.grounded-search.embedding.model"
        private const val TIMEOUT_KEY = "app.grounded-search.embedding.timeout-seconds"
        private const val ENABLED_KEY = "app.llm.openai.enabled"
        private const val API_KEY = "app.llm.openai.api-key"
        private const val BASE_URL = "app.llm.openai.base-url"
    }
}
