package com.sleekydz86.idolglow.global.llm

import com.fasterxml.jackson.annotation.JsonInclude
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class LangfuseIngestionBatch(
    val batch: List<LangfuseIngestionEvent>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class LangfuseIngestionEvent(
    val id: String,
    val type: String,
    val timestamp: String,
    val body: LangfuseObservationBody,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class LangfuseObservationBody(
    val id: String,
    val name: String,
    val traceId: String? = null,
    val timestamp: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val model: String? = null,
    val input: String? = null,
    val output: String? = null,
    val usageDetails: LangfuseUsage? = null,
    val level: String? = null,
    val statusMessage: String? = null,
    val version: String? = null,
    val tags: List<String>? = null,
    val metadata: LangfuseCallMetadata,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class LangfuseUsage(
    val input: Int?,
    val output: Int?,
)

data class LangfuseCallMetadata(
    val provider: String,
    val outcome: String,
)

data class LangfuseContent(
    val input: String?,
    val output: String?,
)

object LangfuseIngestionPayload {
    private const val SUCCESS: String = "success"

    fun build(
        trace: LlmCallTrace,
        content: LangfuseContent,
        newId: () -> String = { UUID.randomUUID().toString() },
    ): LangfuseIngestionBatch {
        val traceId = newId()
        val started = trace.startedAt.toString()
        val metadata = LangfuseCallMetadata(provider = trace.provider, outcome = trace.outcome)
        val traceBody =
            LangfuseObservationBody(
                id = traceId,
                name = trace.feature,
                timestamp = started,
                input = content.input,
                output = content.output,
                version = trace.promptVersion,
                tags = listOf(trace.feature, trace.provider),
                metadata = metadata,
            )
        return LangfuseIngestionBatch(
            listOf(
                LangfuseIngestionEvent(newId(), "trace-create", started, traceBody),
                LangfuseIngestionEvent(
                    newId(),
                    "generation-create",
                    started,
                    generation(trace, content, traceId, newId()),
                ),
            ),
        )
    }

    private fun generation(
        trace: LlmCallTrace,
        content: LangfuseContent,
        traceId: String,
        generationId: String,
    ): LangfuseObservationBody =
        LangfuseObservationBody(
            id = generationId,
            name = trace.feature,
            traceId = traceId,
            startTime = trace.startedAt.toString(),
            endTime = trace.endedAt.toString(),
            model = trace.completion?.model,
            input = content.input,
            output = content.output,
            usageDetails = usage(trace.completion),
            level = if (trace.outcome == SUCCESS) "DEFAULT" else "ERROR",
            statusMessage = trace.errorType ?: trace.outcome,
            version = trace.promptVersion,
            metadata = LangfuseCallMetadata(provider = trace.provider, outcome = trace.outcome),
        )

    private fun usage(completion: LlmCompletion?): LangfuseUsage? {
        val input = completion?.inputTokens
        val output = completion?.outputTokens
        return if (input == null && output == null) null else LangfuseUsage(input, output)
    }
}
