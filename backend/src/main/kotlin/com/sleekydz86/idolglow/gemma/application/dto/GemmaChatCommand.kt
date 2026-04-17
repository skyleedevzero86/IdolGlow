package com.sleekydz86.idolglow.gemma.application.dto

data class GemmaChatCommand(
    val prompt: String,
    val systemPrompt: String? = null,
    val imageUrl: String? = null,
    val enableThinking: Boolean = false,
    val temperature: Double = 1.0,
    val topP: Double = 0.95,
    val maxTokens: Int = 512,
    val model: String? = null,
)
