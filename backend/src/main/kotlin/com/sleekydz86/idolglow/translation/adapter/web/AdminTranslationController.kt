package com.sleekydz86.idolglow.translation.adapter.web

import com.sleekydz86.idolglow.translation.adapter.web.request.GlossaryTermRequest
import com.sleekydz86.idolglow.translation.adapter.web.request.TranslationEditRequest
import com.sleekydz86.idolglow.translation.application.TranslationAdminService
import com.sleekydz86.idolglow.translation.application.TranslationGlossaryService
import com.sleekydz86.idolglow.translation.application.dto.GlossaryTermResult
import com.sleekydz86.idolglow.translation.application.dto.TranslationRowResult
import com.sleekydz86.idolglow.translation.application.dto.TranslationSummaryResult
import com.sleekydz86.idolglow.translation.domain.GlossaryMode
import com.sleekydz86.idolglow.translation.domain.TranslationContentType
import com.sleekydz86.idolglow.translation.domain.TranslationStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Locale

@Tag(name = "관리자 번역", description = "번역 검수·승인·직접 수정·용어집 관리 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/translations")
class AdminTranslationController(
    private val adminService: TranslationAdminService,
    private val glossaryService: TranslationGlossaryService,
) {
    @Operation(summary = "상태별 번역 목록")
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "REVIEW") status: String,
        @RequestParam(required = false) type: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<List<TranslationRowResult>> =
        ResponseEntity.ok(
            adminService.list(
                parseStatus(status),
                type?.takeIf { it.isNotBlank() }?.let(TranslationContentType::parse),
                page,
                size,
            ),
        )

    @Operation(summary = "번역 현황 합계")
    @GetMapping("/summary")
    fun summary(): ResponseEntity<List<TranslationSummaryResult>> = ResponseEntity.ok(adminService.summary())

    @Operation(summary = "번역 상세 (원문 포함)")
    @GetMapping("/{id}")
    fun detail(
        @PathVariable id: Long,
    ): ResponseEntity<TranslationRowResult> = ResponseEntity.ok(adminService.detail(id))

    @Operation(summary = "검수 대기 번역 승인")
    @PostMapping("/{id}/approve")
    fun approve(
        @PathVariable id: Long,
    ): ResponseEntity<TranslationRowResult> = ResponseEntity.ok(adminService.approve(id))

    @Operation(summary = "번역 직접 수정 후 노출")
    @PutMapping("/{id}")
    fun edit(
        @PathVariable id: Long,
        @RequestBody request: TranslationEditRequest,
    ): ResponseEntity<TranslationRowResult> = ResponseEntity.ok(adminService.edit(id, request.fields))

    @Operation(summary = "번역 다시 대기열에 넣기")
    @PostMapping("/{id}/requeue")
    fun requeue(
        @PathVariable id: Long,
    ): ResponseEntity<TranslationRowResult> = ResponseEntity.ok(adminService.requeue(id))

    @Operation(summary = "용어집 목록")
    @GetMapping("/glossary")
    fun glossary(): ResponseEntity<List<GlossaryTermResult>> = ResponseEntity.ok(glossaryService.list())

    @Operation(summary = "용어 추가")
    @PostMapping("/glossary")
    fun addGlossary(
        @RequestBody request: GlossaryTermRequest,
    ): ResponseEntity<GlossaryTermResult> =
        ResponseEntity.ok(
            glossaryService.add(request.sourceTerm, parseMode(request.mode), request.locale, request.targetTerm),
        )

    @Operation(summary = "용어 삭제")
    @DeleteMapping("/glossary/{id}")
    fun deleteGlossary(
        @PathVariable id: Long,
    ): ResponseEntity<Unit> {
        glossaryService.delete(id)
        return ResponseEntity.noContent().build()
    }

    private fun parseStatus(raw: String): TranslationStatus =
        TranslationStatus.entries.firstOrNull { it.name == raw.trim().uppercase(Locale.ROOT) }
            ?: throw IllegalArgumentException("번역 상태가 올바르지 않습니다: ${raw.take(STATUS_ECHO)}")

    private fun parseMode(raw: String): GlossaryMode =
        GlossaryMode.entries.firstOrNull { it.name == raw.trim().uppercase(Locale.ROOT) }
            ?: throw IllegalArgumentException("용어 방식은 FIXED 또는 KEEP 이어야 합니다.")

    companion object {
        private const val STATUS_ECHO = 20
    }
}
