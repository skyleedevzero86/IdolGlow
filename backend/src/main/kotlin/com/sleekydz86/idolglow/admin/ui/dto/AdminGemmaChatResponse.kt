package com.sleekydz86.idolglow.admin.ui.dto

import com.sleekydz86.idolglow.gemma.application.dto.GemmaChatResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관리자 Gemma 채팅 테스트 응답")
data class AdminGemmaChatResponse(
    @field:Schema(description = "실제 호출에 사용된 모델명", example = "google/gemma-4-E4B-it")
    val model: String,

    @field:Schema(description = "어시스턴트 최종 답변")
    val answer: String,

    @field:Schema(description = "종료 사유", example = "stop")
    val finishReason: String?,

    @field:Schema(description = "프롬프트 토큰 수", example = "128")
    val promptTokens: Int?,

    @field:Schema(description = "생성 토큰 수", example = "212")
    val completionTokens: Int?,

    @field:Schema(description = "총 토큰 수", example = "340")
    val totalTokens: Int?,

    @field:Schema(description = "사고(thinking) 모드 사용 여부", example = "false")
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
