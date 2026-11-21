package com.sleekydz86.idolglow.partner.adapter.web

import com.sleekydz86.idolglow.partner.adapter.web.request.PartnerEnabledRequest
import com.sleekydz86.idolglow.partner.adapter.web.request.PartnerIssueRequest
import com.sleekydz86.idolglow.partner.application.PartnerAdminService
import com.sleekydz86.idolglow.partner.application.dto.PartnerIssued
import com.sleekydz86.idolglow.partner.application.dto.PartnerSummary
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 파트너", description = "파트너 키 발급·목록·사용 중지")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/partners")
class AdminPartnerController(
    private val partnerAdminService: PartnerAdminService,
) {
    @Operation(summary = "파트너 키 발급", description = "응답의 apiKey는 이때 한 번만 보여 준다. 서버에는 해시만 남는다")
    @PostMapping
    fun issue(
        @RequestBody body: PartnerIssueRequest,
    ): ResponseEntity<PartnerIssued> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(partnerAdminService.issue(body.name, body.locales, body.rateLimitPerMinute))

    @Operation(summary = "파트너 목록", description = "키 원문과 해시는 주지 않는다")
    @GetMapping
    fun list(): ResponseEntity<List<PartnerSummary>> = ResponseEntity.ok(partnerAdminService.list())

    @Operation(summary = "파트너 사용 여부 변경", description = "false로 바꾸면 그 키는 403")
    @PatchMapping("/{id}/enabled")
    fun changeEnabled(
        @PathVariable id: Long,
        @RequestBody body: PartnerEnabledRequest,
    ): ResponseEntity<PartnerSummary> = ResponseEntity.ok(partnerAdminService.changeEnabled(id, body.enabled))
}
