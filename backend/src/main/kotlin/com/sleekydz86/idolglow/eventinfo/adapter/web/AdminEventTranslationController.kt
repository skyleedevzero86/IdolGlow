package com.sleekydz86.idolglow.eventinfo.adapter.web

import com.sleekydz86.idolglow.eventinfo.adapter.web.dto.EventTranslationRequest
import com.sleekydz86.idolglow.eventinfo.adapter.web.dto.EventTranslationResponse
import com.sleekydz86.idolglow.eventinfo.application.EventTranslationTxService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 공연 번역", description = "대표 공연의 언어별 제목을 사람 번역으로 저장한다")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/events/{globalId}/translations")
class AdminEventTranslationController(
    private val eventTranslationTxService: EventTranslationTxService,
) {
    @Operation(
        summary = "사람 번역 저장",
        description =
            "locale은 en, ja, zh 중 하나다. 같은 언어의 공식·AI 번역이 있어도 덮어쓴다. " +
                "제목은 비울 수 없고 255자(문자 수 기준, 255 포함) 이하다. 형식이 틀리면 400, 대표 공연이 없으면 404",
    )
    @PutMapping("/{locale}")
    fun save(
        @PathVariable globalId: String,
        @PathVariable locale: String,
        @RequestBody body: EventTranslationRequest,
    ): ResponseEntity<EventTranslationResponse> {
        val saved = eventTranslationTxService.saveManual(globalId, locale, body.title)
        return ResponseEntity.ok(EventTranslationResponse.from(saved))
    }
}
