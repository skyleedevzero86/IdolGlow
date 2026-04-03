package com.sleekydz86.idolglow.gemma.application.dto

data class GemmaChatResult(
    val model: String,
    val answer: String,
    val finishReason: String?,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?,
    val thinkingEnabled: Boolean,
)
