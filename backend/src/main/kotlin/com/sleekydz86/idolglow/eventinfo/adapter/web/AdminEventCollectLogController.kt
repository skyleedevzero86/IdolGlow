package com.sleekydz86.idolglow.eventinfo.adapter.web

import com.sleekydz86.idolglow.eventinfo.application.EventCollectLogTimeZoneBackfillService
import com.sleekydz86.idolglow.eventinfo.application.TimeZoneBackfillResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 수집 기록", description = "수집 기록 칸을 서비스 중단 없이 바꾸는 작업")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/event-meta/collect-log")
class AdminEventCollectLogController(
    private val eventCollectLogTimeZoneBackfillService: EventCollectLogTimeZoneBackfillService,
) {
    @Operation(
        summary = "수집 시각 새 칸 채우기",
        description = "새 칸이 비어 있는 기록만 정해진 건수씩 나눠 채운다. 이미 채워진 값은 바꾸지 않고, 다시 실행하면 남은 것만 이어서 채운다",
    )
    @PostMapping("/time-zone-backfill")
    fun backfill(
        @RequestParam(defaultValue = "1000") batchSize: Int,
    ): ResponseEntity<TimeZoneBackfillResult> =
        ResponseEntity.ok(eventCollectLogTimeZoneBackfillService.backfill(batchSize))
}
