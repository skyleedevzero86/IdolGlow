package com.sleekydz86.idolglow.admin.ui.request

import com.sleekydz86.idolglow.gemma.application.dto.GemmaChatCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

@Schema(description = "관리자 Gemma 채팅 테스트 요청")
data class AdminGemmaChatRequest(
    @field:NotBlank
    @field:Schema(
        description = "모델에 전달할 사용자 프롬프트",
        example = "고객에게 전달할 취소 사유 문구를 부드럽게 다듬어 주세요."
    )
    val prompt: String,

    @field:Schema(
        description = "선택 시스템 프롬프트. 비우면 백엔드 기본 한국어 어시스턴트 프롬프트를 사용합니다.",
        example = "IdolGlow 관리자를 돕는 어시스턴트입니다. 한국어로 답변하세요."
    )
    val systemPrompt: String? = null,

    @field:Schema(
        description = "멀티모달 요청 시 선택 이미지 URL",
        example = "https://example.com/admin-screen.png"
    )
    val imageUrl: String? = null,

    @field:Schema(description = "Gemma 사고(thinking) 모드 사용 여부", example = "false")
    val enableThinking: Boolean = false,

    @field:DecimalMin("0.0")
    @field:DecimalMax("2.0")
    @field:Schema(description = "샘플링 temperature", example = "1.0", minimum = "0.0", maximum = "2.0")
    val temperature: Double = 1.0,

    @field:DecimalMin("0.0")
    @field:DecimalMax("1.0")
    @field:Schema(description = "샘플링 top_p", example = "0.95", minimum = "0.0", maximum = "1.0")
    val topP: Double = 0.95,

    @field:Min(1)
    @field:Max(2048)
    @field:Schema(description = "최대 출력 토큰 수", example = "512", minimum = "1", maximum = "2048")
    val maxTokens: Int = 512,

    @field:Schema(
        description = "선택 모델명. 비우면 app.ai.gemma.model 설정값을 사용합니다.",
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
