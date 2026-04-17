package com.sleekydz86.idolglow.admin.ui

import com.sleekydz86.idolglow.admin.ui.dto.AdminGemmaChatResponse
import com.sleekydz86.idolglow.admin.ui.request.AdminGemmaChatRequest
import com.sleekydz86.idolglow.admin.ui.request.toCommand
import com.sleekydz86.idolglow.gemma.application.GemmaChatService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 Gemma", description = "Gemma E4B 채팅 테스트 API")
@RestController
@RequestMapping("/admin/ai/gemma")
@PreAuthorize("hasRole('ADMIN')")
class AdminGemmaController(
    private val gemmaChatService: GemmaChatService,
) {

    @Operation(
        summary = "Gemma 채팅 테스트",
        description = "OpenAI 호환 /chat/completions 엔드포인트로 Gemma 모델을 호출합니다. 텍스트만 보내거나 imageUrl을 함께 보내 멀티모달 요청을 할 수 있습니다.",
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/chat")
    fun chat(
        @Valid @RequestBody request: AdminGemmaChatRequest,
    ): ResponseEntity<AdminGemmaChatResponse> =
        ResponseEntity.ok(
            AdminGemmaChatResponse.from(
                gemmaChatService.chat(request.toCommand())
            )
        )
}
