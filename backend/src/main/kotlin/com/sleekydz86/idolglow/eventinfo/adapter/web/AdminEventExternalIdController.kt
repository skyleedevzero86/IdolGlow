package com.sleekydz86.idolglow.eventinfo.adapter.web

import com.sleekydz86.idolglow.eventinfo.adapter.web.dto.ExternalIdLinkRequest
import com.sleekydz86.idolglow.eventinfo.application.EventExternalIdService
import com.sleekydz86.idolglow.eventinfo.application.ExternalIdView
import com.sleekydz86.idolglow.eventinfo.domain.ExternalIdScheme
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 공연 외부 ID", description = "대표 공연에 위키데이터 같은 국제 표준 ID를 붙이고 뗀다")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/events/{globalId}/external-ids")
class AdminEventExternalIdController(
    private val eventExternalIdService: EventExternalIdService,
) {
    @Operation(summary = "외부 ID 목록", description = "공급처에서 자동으로 오는 ID와 관리자가 붙인 ID를 함께 보여 준다")
    @GetMapping
    fun list(
        @PathVariable globalId: String,
    ): ResponseEntity<List<ExternalIdView>> =
        ResponseEntity.ok(eventExternalIdService.idsOf(listOf(globalId))[globalId].orEmpty())

    @Operation(
        summary = "외부 ID 붙이기",
        description = "지금은 WIKIDATA만 직접 붙일 수 있다. 형식이 틀리면 400, 다른 공연에 이미 붙어 있으면 409",
    )
    @PostMapping
    fun attach(
        @PathVariable globalId: String,
        @RequestBody body: ExternalIdLinkRequest,
    ): ResponseEntity<ExternalIdView> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(eventExternalIdService.attach(globalId, ExternalIdScheme.parse(body.scheme), body.externalId))

    @Operation(summary = "외부 ID 떼기", description = "이 공연에 붙어 있지 않으면 404")
    @DeleteMapping("/{scheme}/{externalId}")
    fun detach(
        @PathVariable globalId: String,
        @PathVariable scheme: String,
        @PathVariable externalId: String,
    ): ResponseEntity<Unit> {
        eventExternalIdService.detach(globalId, ExternalIdScheme.parse(scheme), externalId)
        return ResponseEntity.noContent().build()
    }
}
