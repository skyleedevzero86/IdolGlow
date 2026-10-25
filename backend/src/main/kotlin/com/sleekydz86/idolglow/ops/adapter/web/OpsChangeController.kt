package com.sleekydz86.idolglow.ops.adapter.web

import com.sleekydz86.idolglow.ops.adapter.web.dto.OpsChangeResponse
import com.sleekydz86.idolglow.ops.adapter.web.dto.toResponse
import com.sleekydz86.idolglow.ops.adapter.web.request.CreateOpsChangeBody
import com.sleekydz86.idolglow.ops.application.OpsChangeCommand
import com.sleekydz86.idolglow.ops.application.OpsChangeCommandService
import com.sleekydz86.idolglow.ops.domain.OpsActorKind
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "운영 변경", description = "운영 변경 요청·승인·적용·되돌리기")
@RestController
@RequestMapping("/admin/ops-changes")
class OpsChangeController(
    private val opsChangeCommandService: OpsChangeCommandService,
) {
    @Operation(summary = "운영 변경 목록", description = "최근 요청 50건")
    @PreAuthorize("hasAnyRole('OPERATOR','APPROVER','ADMIN')")
    @GetMapping
    fun list(): ResponseEntity<List<OpsChangeResponse>> =
        ResponseEntity.ok(opsChangeCommandService.findRecent().map { it.toResponse() })

    @Operation(summary = "운영 변경 요청", description = "OPERATOR 또는 ADMIN. 요청자와 승인자는 달라야 한다")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    @PostMapping
    fun request(
        @RequestBody body: CreateOpsChangeBody,
    ): ResponseEntity<OpsChangeResponse> {
        val kind = actorKind(body.requestedByKind)
        val saved =
            opsChangeCommandService.request(
                OpsChangeCommand(
                    type = body.type,
                    target = body.target,
                    payload = body.payload,
                    kind = kind,
                    agentName = body.requestedBy,
                ),
            )
        return ResponseEntity.ok(saved.toResponse())
    }

    @Operation(summary = "운영 변경 승인", description = "APPROVER 또는 ADMIN. 자기가 만든 요청은 승인할 수 없다")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    @PostMapping("/{requestId}/approve")
    fun approve(
        @PathVariable requestId: Long,
    ): ResponseEntity<OpsChangeResponse> = ResponseEntity.ok(opsChangeCommandService.approve(requestId).toResponse())

    @Operation(summary = "운영 변경 거절")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    @PostMapping("/{requestId}/reject")
    fun reject(
        @PathVariable requestId: Long,
    ): ResponseEntity<OpsChangeResponse> = ResponseEntity.ok(opsChangeCommandService.reject(requestId).toResponse())

    @Operation(summary = "운영 변경 적용", description = "승인된 요청만 적용하고 이전 상태를 남긴다")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    @PostMapping("/{requestId}/apply")
    fun apply(
        @PathVariable requestId: Long,
    ): ResponseEntity<OpsChangeResponse> = ResponseEntity.ok(opsChangeCommandService.apply(requestId).toResponse())

    @Operation(summary = "운영 변경 되돌리기", description = "적용 직전 매핑으로 복구한다")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    @PostMapping("/{requestId}/revert")
    fun revert(
        @PathVariable requestId: Long,
    ): ResponseEntity<OpsChangeResponse> = ResponseEntity.ok(opsChangeCommandService.revert(requestId).toResponse())

    private fun actorKind(raw: String?): OpsActorKind {
        val token = raw?.trim()?.takeIf { it.isNotEmpty() } ?: OpsActorKind.USER.name
        return runCatching { OpsActorKind.valueOf(token) }.getOrElse {
            throw IllegalArgumentException("요청자 종류는 USER 또는 AGENT 여야 합니다. requestedByKind=$raw")
        }
    }
}
