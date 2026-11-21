package com.sleekydz86.idolglow.global.llm

data class LlmRequest(
    val feature: String,
    val systemPrompt: String,
    val userPrompt: String,
    val jsonOutput: Boolean = true,
    val maxOutputTokens: Int = 1024,
    val temperature: Double = 0.2,
    val promptVersion: String? = null,
)

data class LlmResult(
    val text: String,
    val provider: String,
    val latencyMs: Long,
)

data class LlmCompletion(
    val text: String,
    val model: String? = null,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
)

interface LlmProvider {
    val name: String
    val external: Boolean

    fun isConfigured(): Boolean

    fun complete(request: LlmRequest): LlmCompletion?
}
