package com.sleekydz86.idolglow.subway.infrastructure.enrichment

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper

@Component
class OpenAiSubwayStationSummaryClient(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    @Value("\${app.llm.openai.enabled:false}")
    private val openAiEnabled: Boolean,
    @Value("\${app.llm.openai.base-url:https://api.openai.com}")
    private val baseUrl: String,
    @Value("\${app.llm.openai.api-key:}")
    private val apiKey: String,
    @Value("\${app.llm.openai.model:gpt-4.1-mini}")
    private val model: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generate(
        lineId: String,
        lineName: String,
        stationCd: String,
        stationDisplayName: String,
    ): LlmSubwayStationSummary? {
        if (!openAiEnabled) {
            return null
        }
        val normalizedApiKey = apiKey.trim()
        if (normalizedApiKey.isEmpty()) {
            return null
        }

        val request =
            SubwayChatCompletionRequest(
                model = model,
                temperature = 0.45,
                responseFormat = SubwayResponseFormat(type = "json_object"),
                messages =
                    listOf(
                        SubwayChatMessage(
                            role = "system",
                            content = SubwayStationSummaryPrompts.system,
                        ),
                        SubwayChatMessage(
                            role = "user",
                            content =
                                SubwayStationSummaryPrompts.userJson(
                                    objectMapper,
                                    lineId,
                                    lineName,
                                    stationCd,
                                    stationDisplayName,
                                ),
                        ),
                    ),
            )

        val raw =
            try {
                webClient
                    .post()
                    .uri("${baseUrl.trimEnd('/')}/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $normalizedApiKey")
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .bodyValue(request)
                    .exchangeToMono { response ->
                        response
                            .bodyToMono(String::class.java)
                            .defaultIfEmpty("")
                            .map { body -> RawSubwayOpenAiResponse(response.statusCode(), body) }
                    }.block()
            } catch (e: Exception) {
                log.warn("지하철 역 요약 OpenAI 호출 실패: {}", e.message)
                null
            } ?: return null

        if (!raw.statusCode.is2xxSuccessful) {
            log.warn("지하철 역 요약 OpenAI 비정상 응답: status={} body={}", raw.statusCode, raw.body.take(500))
            return null
        }

        val content =
            runCatching {
                objectMapper
                    .readValue(raw.body, SubwayChatCompletionResponse::class.java)
                    .choices
                    .firstOrNull()
                    ?.message
                    ?.content
            }.getOrNull() ?: return null

        val json =
            content
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

        return runCatching {
            objectMapper.readValue(json, LlmSubwayStationSummary::class.java).normalize(stationDisplayName)
        }.getOrElse { e ->
            log.warn("지하철 역 요약 OpenAI JSON 파싱 실패: {}", e.message)
            null
        }
    }

    companion object {
        const val CACHE_NAME: String = "subway-station-llm-summary"
    }
}
