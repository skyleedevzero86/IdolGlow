package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.application.port.RerankPort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSearchHit
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.ObjectMapper

@Component
@Profile("!test")
@ConditionalOnProperty(name = ["app.grounded-search.rerank.enabled"], havingValue = "true")
class CohereRerankAdapter(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val settings: GroundedRemoteSettings,
    private val guard: GroundedCallGuard,
) : RerankPort {
    override fun rerank(query: String, hits: List<GroundedSearchHit>): List<GroundedSearchHit> {
        if (hits.isEmpty()) {
            return hits
        }
        return ranked(query, hits)
    }

    private fun ranked(query: String, hits: List<GroundedSearchHit>): List<GroundedSearchHit> {
        if (!ready()) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "근거 검색 재정렬 설정이 없습니다. 키를 확인해 주세요.")
        }
        val body = guard.run("재정렬") { post(query, hits) }
        return runCatching { order(hits, body) }.getOrElse { error ->
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "근거 검색 재정렬 응답을 해석하지 못했습니다.", error)
        }
    }

    private fun post(query: String, hits: List<GroundedSearchHit>): String =
        webClient
            .post()
            .uri("${settings.text(BASE_URL).trimEnd('/')}/v2/rerank")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${settings.text(API_KEY)}")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .bodyValue(GroundedCohereRequest(settings.text(MODEL), query, hits.map { it.content }))
            .retrieve()
            .bodyToMono(String::class.java)
            .timeout(settings.rerankTimeout())
            .block()
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "근거 검색 재정렬 응답이 비어 있습니다.")

    private fun order(hits: List<GroundedSearchHit>, body: String): List<GroundedSearchHit> {
        val results = objectMapper.readTree(body).path("results")
        if (!results.isArray || results.isEmpty) {
            return hits
        }
        return results.mapNotNull { node ->
            val index = node.path("index").asInt(-1)
            hits.getOrNull(index)?.copy(rerankScore = node.path("relevance_score").asDouble())
        }
    }

    private fun ready(): Boolean = settings.text(API_KEY).isNotBlank()

    private companion object {
        private const val API_KEY = "app.grounded-search.rerank.api-key"
        private const val BASE_URL = "app.grounded-search.rerank.base-url"
        private const val MODEL = "app.grounded-search.rerank.model"
    }
}
