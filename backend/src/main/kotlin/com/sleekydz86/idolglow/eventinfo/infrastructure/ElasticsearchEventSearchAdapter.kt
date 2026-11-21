package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.EventSearchProperties
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSearchIndexPort
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchDocument
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchHit
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchIndexOutcome
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

@Component
class ElasticsearchEventSearchAdapter(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val properties: EventSearchProperties,
) : EventSearchIndexPort {
    private val indexReady = AtomicBoolean(false)

    override fun index(
        document: EventSearchDocument,
        overwriteSameVersion: Boolean,
    ): EventSearchIndexOutcome {
        ensureIndex()
        val versionType = if (overwriteSameVersion) "external_gte" else "external"
        val query = "version=${document.metaVersion}&version_type=$versionType"
        val path = "/${properties.index}/_doc/${document.globalId}?$query"
        val response = send(HttpMethod.PUT, path, ElasticsearchEventSearchBodies.source(document))
        return when {
            response.status == CONFLICT -> EventSearchIndexOutcome.STALE
            response.ok -> EventSearchIndexOutcome.INDEXED
            else -> throw failure("검색 색인 저장 실패. globalId=${document.globalId}", response)
        }
    }

    override fun search(
        query: String,
        size: Int,
    ): List<EventSearchHit> {
        ensureIndex()
        val body = ElasticsearchEventSearchBodies.search(query, size)
        val response = send(HttpMethod.POST, "/${properties.index}/_search", body)
        if (!response.ok) throw failure("공연 검색 실패", response)
        val hits = objectMapper.readTree(response.body).path("hits").path("hits")
        return (0 until hits.size()).map { ElasticsearchEventSearchBodies.hit(hits.get(it).path("_source")) }
    }

    fun refresh() {
        val response = send(HttpMethod.POST, "/${properties.index}/_refresh", null)
        if (!response.ok) throw failure("검색 색인 새로 고침 실패", response)
    }

    private fun ensureIndex() {
        if (indexReady.get()) return
        val exists = send(HttpMethod.HEAD, "/${properties.index}", null)
        if (exists.status == NOT_FOUND) {
            val created = send(HttpMethod.PUT, "/${properties.index}", ElasticsearchEventSearchBodies.indexDefinition())
            val alreadyThere = created.body.contains("resource_already_exists_exception")
            if (!created.ok && !alreadyThere) throw failure("검색 색인 만들기 실패", created)
        } else if (!exists.ok) {
            throw failure("검색 색인 확인 실패", exists)
        }
        indexReady.set(true)
    }

    private fun send(
        method: HttpMethod,
        path: String,
        body: Any?,
    ): SearchResponse {
        val request = webClient.method(method).uri(properties.baseUrl.trimEnd('/') + path)
        val withBody =
            if (body == null) {
                request
            } else {
                request.contentType(MediaType.APPLICATION_JSON).bodyValue(objectMapper.writeValueAsString(body))
            }
        return withBody
            .exchangeToMono { response ->
                response
                    .bodyToMono(String::class.java)
                    .defaultIfEmpty("")
                    .map { SearchResponse(response.statusCode().value(), it) }
            }.timeout(Duration.ofSeconds(properties.timeoutSeconds))
            .onErrorMap(WebClientRequestException::class.java) { IllegalStateException("검색 엔진 연결 실패. path=$path", it) }
            .onErrorMap(TimeoutException::class.java) { IllegalStateException("검색 엔진 응답 시간 초과. path=$path", it) }
            .block() ?: throw IllegalStateException("검색 엔진 응답이 비어 있습니다. path=$path")
    }

    private fun failure(
        message: String,
        response: SearchResponse,
    ): IllegalStateException {
        val preview = response.body.take(BODY_PREVIEW)
        return IllegalStateException("$message. status=${response.status} body=$preview")
    }

    private data class SearchResponse(
        val status: Int,
        val body: String,
    ) {
        val ok: Boolean get() = status in SUCCESS_RANGE
    }

    companion object {
        private const val CONFLICT: Int = 409
        private const val NOT_FOUND: Int = 404
        private val SUCCESS_RANGE: IntRange = 200..299
        private const val BODY_PREVIEW: Int = 300
    }
}
