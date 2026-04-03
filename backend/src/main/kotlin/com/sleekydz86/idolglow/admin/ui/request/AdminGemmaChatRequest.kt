package com.sleekydz86.idolglow.admin.ui.request

import com.sleekydz86.idolglow.gemma.application.dto.GemmaChatCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

@Schema(description = "Admin request for Gemma chat test")
data class AdminGemmaChatRequest(
    @field:NotBlank
    @field:Schema(
        description = "User prompt sent to the model",
        example = "Rewrite this cancellation reason in a softer tone for customers."
    )
    val prompt: String,

    @field:Schema(
        description = "Optional system prompt. When blank, the backend default Korean assistant prompt is used.",
        example = "You are a helpful assistant for IdolGlow admins. Answer in Korean."
    )
    val systemPrompt: String? = null,

    @field:Schema(
        description = "Optional image URL for multimodal requests.",
        example = "https://example.com/admin-screen.png"
    )
    val imageUrl: String? = null,

    @field:Schema(description = "Enable Gemma thinking mode", example = "false")
    val enableThinking: Boolean = false,

    @field:DecimalMin("0.0")
    @field:DecimalMax("2.0")
    @field:Schema(description = "Sampling temperature", example = "1.0", minimum = "0.0", maximum = "2.0")
    val temperature: Double = 1.0,

    @field:DecimalMin("0.0")
    @field:DecimalMax("1.0")
    @field:Schema(description = "Sampling top_p", example = "0.95", minimum = "0.0", maximum = "1.0")
    val topP: Double = 0.95,

    @field:Min(1)
    @field:Max(2048)
    @field:Schema(description = "Maximum output tokens", example = "512", minimum = "1", maximum = "2048")
    val maxTokens: Int = 512,

    @field:Schema(
        description = "Optional model name. When blank, app.ai.gemma.model is used.",
        example = "google/gemma-4-E4B-it"
    )
    val model: String? = null,
)

fun AdminGemmaChatRequest.toCommand(): GemmaChatCommand =
    GemmaChatCommand(
        prompt = prompt,
        systemPrompt = systemPrompt,
        imageUrl = imageUrl,
        enableThinking = enableThinking,
        temperature = temperature,
        topP = topP,
        maxTokens = maxTokens,
        model = model,
    )
