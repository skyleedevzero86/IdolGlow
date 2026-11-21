package com.sleekydz86.idolglow.eventinfo.adapter.web

import com.sleekydz86.idolglow.eventinfo.application.EventMetaDeadLetterReprocessed
import com.sleekydz86.idolglow.eventinfo.application.EventMetaDeadLetterService
import com.sleekydz86.idolglow.eventinfo.application.EventMetaDeadLetterView
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 공연 메타 실패 메시지", description = "Kafka 소비자가 3번 연속 실패한 공연 메타 이벤트 조회·다시 처리")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/event-meta/dead-letters")
class AdminEventMetaDeadLetterController(
    private val eventMetaDeadLetterService: EventMetaDeadLetterService,
) {
    @Operation(summary = "실패 메시지 목록", description = "최근 100건. pendingOnly=true면 아직 다시 처리하지 않은 것만")
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "true") pendingOnly: Boolean,
    ): ResponseEntity<List<EventMetaDeadLetterView>> = ResponseEntity.ok(eventMetaDeadLetterService.list(pendingOnly))

    @Operation(summary = "실패 메시지 다시 처리", description = "원래 소비자로 한 번 더 처리한다. 이미 반영된 이벤트면 DUPLICATE로 끝난다")
    @PostMapping("/{id}/reprocess")
    fun reprocess(
        @PathVariable id: Long,
    ): ResponseEntity<EventMetaDeadLetterReprocessed> = ResponseEntity.ok(eventMetaDeadLetterService.reprocess(id))
}
