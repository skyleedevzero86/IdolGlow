package com.sleekydz86.idolglow.eventinfo.adapter.web

import com.sleekydz86.idolglow.eventinfo.application.EventSessionQueryService
import com.sleekydz86.idolglow.eventinfo.application.EventSessionsOfSource
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "공연 회차", description = "공연 상세 화면에서 대표 공연 아래 회차를 본다")
@RestController
@RequestMapping("/api/event-info/sessions")
class EventSessionController(
    private val eventSessionQueryService: EventSessionQueryService,
) {
    @Operation(
        summary = "공급처 원본으로 회차 목록 조회",
        description = "확정 연결된 원본이면 대표 공연의 회차를 준다. 연결이 없으면 globalId=null, 빈 목록. 기본은 오늘부터 90일",
    )
    @GetMapping
    fun sessions(
        @Parameter(description = "공급처. 예: KOPIS_PBLPRFR") @RequestParam source: String,
        @Parameter(description = "공급처 원본 ID") @RequestParam sourceId: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): ResponseEntity<EventSessionsOfSource> = ResponseEntity.ok(eventSessionQueryService.sessionsOfSource(source, sourceId, from, to))
}
