package com.sleekydz86.idolglow.admin.ui.dto

import com.sleekydz86.idolglow.gemma.application.dto.GemmaChatResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Admin response for Gemma chat test")
data class AdminGemmaChatResponse(
    @field:Schema(description = "Resolved model name", example = "google/gemma-4-E4B-it")
    val model: String,

    @field:Schema(description = "Final assistant answer")
    val answer: String,

    @field:Schema(description = "Finish reason", example = "stop")
    val finishReason: String?,

    @field:Schema(description = "Prompt token count", example = "128")
    val promptTokens: Int?,

    @field:Schema(description = "Completion token count", example = "212")
    val completionTokens: Int?,

    @field:Schema(description = "Total token count", example = "340")
    val totalTokens: Int?,

    @field:Schema(description = "Whether thinking mode was enabled", example = "false")
    val thinkingEnabled: Boolean,
) {
    companion object {
        fun from(result: GemmaChatResult): AdminGemmaChatResponse =
            AdminGemmaChatResponse(
                model = result.model,
                answer = result.answer,
                finishReason = result.finishReason,
                promptTokens = result.promptTokens,
                completionTokens = result.completionTokens,
                totalTokens = result.totalTokens,
                thinkingEnabled = result.thinkingEnabled,
            )
    }
}
