package com.sleekydz86.idolglow.global.llm

data class LlmRequest(
    val feature: String,
    val systemPrompt: String,
    val userPrompt: String,
    val jsonOutput: Boolean = true,
    val maxOutputTokens: Int = 1024,
    val temperature: Double = 0.2,
)

data class LlmResult(
    val text: String,
    val provider: String,
    val latencyMs: Long,
)

interface LlmProvider {
    val name: String
    val external: Boolean

    fun isConfigured(): Boolean

    fun complete(request: LlmRequest): String?
}
