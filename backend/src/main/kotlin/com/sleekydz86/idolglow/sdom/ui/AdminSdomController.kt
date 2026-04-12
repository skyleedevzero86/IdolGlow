package com.sleekydz86.idolglow.sdom.ui

import com.sleekydz86.idolglow.sdom.application.SdomQueryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin sdom", description = "관리자 사이트 도메인(tb_domain_list) 선택 옵션 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/sdom")
class AdminSdomController(
    private val sdomQueryService: SdomQueryService,
) {
    @Operation(summary = "도메인 목록 조회")
    @GetMapping
    fun listDomains() = ResponseEntity.ok(sdomQueryService.findAllActiveOrdered())
}
