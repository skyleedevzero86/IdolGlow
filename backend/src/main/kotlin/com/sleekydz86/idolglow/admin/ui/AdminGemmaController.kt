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

@Tag(name = "Admin Gemma", description = "Admin API for Gemma E4B testing")
@RestController
@RequestMapping("/admin/ai/gemma")
@PreAuthorize("hasRole('ADMIN')")
class AdminGemmaController(
    private val gemmaChatService: GemmaChatService,
) {

    @Operation(
        summary = "Gemma chat test",
        description = "Calls a Gemma model through an OpenAI-compatible /chat/completions endpoint. You can send text only or text plus imageUrl for a multimodal request.",
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
