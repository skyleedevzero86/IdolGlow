package com.sleekydz86.idolglow.eventinfo.adapter.web

import com.sleekydz86.idolglow.eventinfo.application.EventSearchItem
import com.sleekydz86.idolglow.eventinfo.application.EventSearchQueryService
import com.sleekydz86.idolglow.eventinfo.domain.EventLocale
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "공연 검색", description = "검색 색인에서 대표 공연을 제목·번역 제목·장소로 찾는다")
@RestController
@RequestMapping("/api/event-info/search")
class EventSearchController(
    private val eventSearchQueryService: EventSearchQueryService,
) {
    @Operation(
        summary = "공연 검색",
        description = "병합된 공연은 빼고 찾는다. 요청 언어의 번역 제목이 있으면 그 제목을 준다. 검색이 꺼져 있거나 검색 엔진이 응답하지 않으면 503",
    )
    @GetMapping
    fun search(
        @Parameter(description = "검색어. 예: 먼지, Seoul Concert") @RequestParam q: String,
        @Parameter(description = "언어. ko, en, ja, zh") @RequestParam(required = false) lang: String?,
        @Parameter(description = "결과 수. 기본 20, 최대 50") @RequestParam(required = false) size: Int?,
        @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) acceptLanguage: String?,
    ): ResponseEntity<List<EventSearchItem>> =
        ResponseEntity.ok(eventSearchQueryService.search(q, EventLocale.requested(lang, acceptLanguage), size))
}
