package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.payment.application.BillingRenewalNoticeService
import com.sleekydz86.idolglow.payment.application.dto.RenewalDecisionRequest
import com.sleekydz86.idolglow.payment.application.dto.RenewalNoticeResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "토스 빌링 갱신 안내", description = "갱신 안내 메일 링크로 연장 여부 선택")
@RestController
@RequestMapping("/payments/toss/billing/renewal-notices")
class TossBillingRenewalNoticeController(
    private val renewalNoticeService: BillingRenewalNoticeService,
) {
    @Operation(summary = "갱신 안내 조회")
    @GetMapping("/{token}")
    fun preview(
        @PathVariable token: String,
    ): ResponseEntity<RenewalNoticeResponse> = ResponseEntity.ok(renewalNoticeService.preview(token))

    @Operation(summary = "연장 또는 이번 기간만 이용 선택")
    @PostMapping("/{token}/decision")
    fun decide(
        @PathVariable token: String,
        @RequestBody request: RenewalDecisionRequest,
    ): ResponseEntity<RenewalNoticeResponse> = ResponseEntity.ok(renewalNoticeService.decide(token, request.decision))
}
