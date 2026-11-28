package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.payment.application.BillingPlanChangeService
import com.sleekydz86.idolglow.payment.application.dto.PlanChangePreviewResponse
import com.sleekydz86.idolglow.payment.application.dto.PlanChangeRequest
import com.sleekydz86.idolglow.payment.application.dto.PlanChangeResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "토스 빌링 이용권 변경", description = "구독 중 이용권 기간을 바로 바꾸고 차액을 결제하거나 적립")
@RestController
@RequestMapping("/payments/toss/billing/subscriptions/plan-change")
class TossBillingPlanChangeController(
    private val billingPlanChangeService: BillingPlanChangeService,
) {
    @Operation(summary = "이용권 즉시 변경 금액 미리보기")
    @GetMapping("/preview")
    fun preview(
        @LoginUser userId: Long,
        @RequestParam cycleMonths: Int,
    ): ResponseEntity<PlanChangePreviewResponse> =
        ResponseEntity.ok(billingPlanChangeService.preview(userId, cycleMonths))

    @Operation(summary = "이용권 즉시 변경과 차액 정산")
    @PostMapping
    fun change(
        @LoginUser userId: Long,
        @Valid @RequestBody request: PlanChangeRequest,
    ): ResponseEntity<PlanChangeResultResponse> =
        ResponseEntity.ok(billingPlanChangeService.change(userId, request.toCommand()))
}
