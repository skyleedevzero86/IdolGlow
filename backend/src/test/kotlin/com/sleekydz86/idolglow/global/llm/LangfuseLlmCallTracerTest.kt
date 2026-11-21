package com.sleekydz86.idolglow.global.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectMapper
import java.net.ConnectException
import java.time.Instant
import java.util.Base64

@DisplayName("Langfuse 호출 추적 전송")
class LangfuseLlmCallTracerTest {
    private val sent = mutableListOf<ClientRequest>()

    private val trace =
        LlmCallTrace(
            feature = "translation",
            provider = "local",
            outcome = "success",
            promptVersion = "v1",
            startedAt = Instant.parse("2026-11-21T10:00:00Z"),
            endedAt = Instant.parse("2026-11-21T10:00:02Z"),
            systemPrompt = "system",
            userPrompt = "문의 user@example.com",
            completion = LlmCompletion("답 010-1234-5678", "qwen", 30, 8),
            errorType = null,
        )

    private fun tracer(
        props: LangfuseProperties,
        response: Mono<ClientResponse> = Mono.just(ClientResponse.create(HttpStatus.MULTI_STATUS).build()),
    ): LangfuseLlmCallTracer {
        val webClient =
            WebClient
                .builder()
                .exchangeFunction { request ->
                    sent += request
                    response
                }.build()
        return LangfuseLlmCallTracer(webClient, ObjectMapper(), LlmInputMasker(), props)
    }

    private fun enabled(
        includeContent: Boolean = true,
        maxContentChars: Int = 2000,
    ) = LangfuseProperties(
        enabled = true,
        host = "http://langfuse.test/",
        publicKey = "pk-test",
        secretKey = "sk-test",
        includeContent = includeContent,
        maxContentChars = maxContentChars,
    )

    @DisplayName("꺼져 있거나 키가 비어 있으면 보내지 않는다")
    @Test
    fun skipsWhenDisabledOrKeysMissing() {
        // given
        val off = tracer(LangfuseProperties(enabled = false, publicKey = "pk", secretKey = "sk"))
        val noKey = tracer(enabled().copy(secretKey = ""))

        // when
        off.record(trace)
        noKey.record(trace)

        // then
        assertTrue(sent.isEmpty())
        assertEquals(false, noKey.isEnabled())
    }

    @DisplayName("켜져 있으면 수집 API로 Basic 인증을 붙여 보낸다")
    @Test
    fun sendsToIngestionApiWithBasicAuth() {
        // given
        val tracer = tracer(enabled())

        // when
        tracer.record(trace)

        // then
        val request = sent.single()
        assertEquals("http://langfuse.test/api/public/ingestion", request.url().toString())
        val expected = "Basic " + Base64.getEncoder().encodeToString("pk-test:sk-test".toByteArray())
        assertEquals(expected, request.headers().getFirst(HttpHeaders.AUTHORIZATION))
    }

    @DisplayName("Langfuse에 연결하지 못해도 예외가 밖으로 나오지 않는다")
    @Test
    fun connectionFailureDoesNotThrow() {
        // given
        val tracer = tracer(enabled(), Mono.error(ConnectException("Connection refused")))

        // when
        // then
        assertDoesNotThrow { tracer.record(trace) }
        assertEquals(1, sent.size)
    }

    @DisplayName("본문은 개인정보를 가리고 길이를 자르며, 본문 기록을 끄면 넣지 않는다")
    @Test
    fun masksAndTruncatesContent() {
        // given
        val withContent = tracer(enabled(maxContentChars = 12))
        val withoutContent = tracer(enabled(includeContent = false))

        // when
        val masked = withContent.content(trace)
        val empty = withoutContent.content(trace)

        // then
        assertEquals("system\n\n문의 <…(생략)", masked.input)
        assertEquals("답 <phone>", masked.output)
        assertNull(empty.input)
        assertNull(empty.output)
    }

    @DisplayName("추적과 생성 기록이 같은 추적 ID로 묶이고 토큰·버전·실패 수준이 들어간다")
    @Test
    fun buildsTraceAndGenerationEvents() {
        // given
        var sequence = 0
        val failed = trace.copy(outcome = "error", completion = null, errorType = "IllegalStateException")

        // when
        val ok = LangfuseIngestionPayload.build(trace, LangfuseContent("in", "out")) { "id-${sequence++}" }
        val error = LangfuseIngestionPayload.build(failed, LangfuseContent(null, null)) { "e-${sequence++}" }

        // then
        val (traceEvent, generation) = ok.batch
        assertEquals(listOf("trace-create", "generation-create"), ok.batch.map { it.type })
        assertEquals(traceEvent.body.id, generation.body.traceId)
        assertEquals(LangfuseUsage(30, 8), generation.body.usageDetails)
        assertEquals("qwen", generation.body.model)
        assertEquals("v1", generation.body.version)
        assertEquals("DEFAULT", generation.body.level)
        val errorGeneration = error.batch[1].body
        assertEquals("ERROR", errorGeneration.level)
        assertEquals("IllegalStateException", errorGeneration.statusMessage)
        assertNull(errorGeneration.usageDetails)
    }
}
