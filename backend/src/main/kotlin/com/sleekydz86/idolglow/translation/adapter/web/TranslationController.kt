package com.sleekydz86.idolglow.translation.adapter.web

import com.sleekydz86.idolglow.eventinfo.domain.EventLocale
import com.sleekydz86.idolglow.translation.adapter.web.request.TranslationLookupRequest
import com.sleekydz86.idolglow.translation.application.TranslationQueryService
import com.sleekydz86.idolglow.translation.application.TranslationReportResult
import com.sleekydz86.idolglow.translation.application.TranslationReportService
import com.sleekydz86.idolglow.translation.application.dto.TranslationLookupResult
import com.sleekydz86.idolglow.translation.application.dto.TranslationTarget
import com.sleekydz86.idolglow.translation.domain.TranslationContentType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "콘텐츠 번역", description = "화면 언어에 맞는 콘텐츠 번역 조회와 번역 오류 신고 API")
@RestController
@RequestMapping("/api/translations")
class TranslationController(
    private val queryService: TranslationQueryService,
    private val reportService: TranslationReportService,
) {
    @Operation(summary = "콘텐츠 번역 조회", description = "번역이 없으면 원문 표시 상태와 함께 번역 대기를 등록합니다.")
    @PostMapping("/lookup")
    fun lookup(
        @RequestParam(required = false) lang: String?,
        @RequestHeader(name = "Accept-Language", required = false) acceptLanguage: String?,
        @RequestBody request: TranslationLookupRequest,
    ): ResponseEntity<List<TranslationLookupResult>> {
        val locale = EventLocale.requested(lang, acceptLanguage)
        val targets = request.items.map { TranslationTarget(TranslationContentType.parse(it.type), it.key.trim()) }
        return ResponseEntity.ok(queryService.lookup(locale, targets))
    }

    @Operation(summary = "번역 오류 신고", description = "신고가 기준 수만큼 쌓이면 번역을 숨기고 검수 대기로 돌립니다.")
    @PostMapping("/{translationId}/reports")
    fun report(
        @PathVariable translationId: Long,
    ): ResponseEntity<TranslationReportResult> = ResponseEntity.ok(reportService.report(translationId))
}
