package com.sleekydz86.idolglow.eventinfo.adapter.web

import com.sleekydz86.idolglow.eventinfo.application.EventSearchReindexResult
import com.sleekydz86.idolglow.eventinfo.application.EventSearchReindexService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 공연 검색 색인", description = "공연 검색 색인을 처음 채우거나 망가졌을 때 다시 만든다")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/event-search")
class AdminEventSearchController(
    private val eventSearchReindexService: EventSearchReindexService,
) {
    @Operation(
        summary = "검색 색인 다시 만들기",
        description = "대표 공연 전부를 지금 버전으로 다시 넣는다. 더 새 버전이 이미 들어 있으면 stale로 센다. 검색이 꺼져 있으면 503",
    )
    @PostMapping("/reindex")
    fun reindex(): ResponseEntity<EventSearchReindexResult> = ResponseEntity.ok(eventSearchReindexService.reindexAll())
}
