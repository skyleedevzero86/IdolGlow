package com.sleekydz86.idolglow.global.llm

import java.time.Instant

data class LlmCallTrace(
    val feature: String,
    val provider: String,
    val outcome: String,
    val promptVersion: String?,
    val startedAt: Instant,
    val endedAt: Instant,
    val systemPrompt: String,
    val userPrompt: String,
    val completion: LlmCompletion?,
    val errorType: String?,
)

interface LlmCallTracer {
    fun isEnabled(): Boolean

    fun record(trace: LlmCallTrace)
}
