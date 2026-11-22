package com.sleekydz86.idolglow.global.llm

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.util.Base64

@Component
class LangfuseLlmCallTracer(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val masker: LlmInputMasker,
    private val props: LangfuseProperties,
) : LlmCallTracer {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun isEnabled(): Boolean =
        props.enabled && props.host.isNotBlank() && props.publicKey.isNotBlank() && props.secretKey.isNotBlank()

    override fun record(trace: LlmCallTrace) {
        if (!isEnabled()) return
        val body = objectMapper.writeValueAsString(LangfuseIngestionPayload.build(trace, content(trace)))
        webClient
            .post()
            .uri("${props.host.trimEnd('/')}$INGESTION_PATH")
            .header(HttpHeaders.AUTHORIZATION, basicAuth())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .toBodilessEntity()
            .timeout(Duration.ofSeconds(props.timeoutSeconds))
            .subscribe({ }, { error -> warnFailure(trace, error) })
    }

    fun content(trace: LlmCallTrace): LangfuseContent {
        if (!props.includeContent) return LangfuseContent(input = null, output = null)
        val input = "${trace.systemPrompt}\n\n${trace.userPrompt}"
        return LangfuseContent(
            input = shorten(masker.mask(input)),
            output = trace.completion?.text?.let { shorten(masker.mask(it)) },
        )
    }

    private fun shorten(text: String): String =
        if (text.length <= props.maxContentChars) text else text.take(props.maxContentChars) + TRUNCATED_MARK

    private fun basicAuth(): String {
        val credentials = "${props.publicKey.trim()}:${props.secretKey.trim()}"
        return "Basic " + Base64.getEncoder().encodeToString(credentials.toByteArray(Charsets.UTF_8))
    }

    private fun warnFailure(
        trace: LlmCallTrace,
        error: Throwable,
    ) {
        log.warn(
            "Langfuse 추적 전송 실패 feature={} provider={} error={} status={}",
            trace.feature,
            trace.provider,
            error.javaClass.simpleName,
            (error as? WebClientResponseException)?.statusCode?.value(),
        )
    }

    companion object {
        private const val INGESTION_PATH: String = "/api/public/ingestion"
        private const val TRUNCATED_MARK: String = "…(생략)"
    }
}
