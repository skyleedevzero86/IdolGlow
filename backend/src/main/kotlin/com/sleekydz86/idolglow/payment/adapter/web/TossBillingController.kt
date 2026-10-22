package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.payment.application.TossBillingService
import com.sleekydz86.idolglow.payment.application.dto.IssueTossBillingKeyRequest
import com.sleekydz86.idolglow.payment.application.dto.RetryTossBillingChargeRequest
import com.sleekydz86.idolglow.payment.application.dto.StartTossBillingSubscriptionRequest
import com.sleekydz86.idolglow.payment.application.dto.TossBillingChargeResponse
import com.sleekydz86.idolglow.payment.application.dto.TossBillingClientConfigResponse
import com.sleekydz86.idolglow.payment.application.dto.TossBillingMethodResponse
import com.sleekydz86.idolglow.payment.application.dto.TossBillingOverviewResponse
import com.sleekydz86.idolglow.payment.application.dto.TossBillingSubscriptionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "토스 빌링", description = "카드 자동결제(빌링키) 등록·구독·승인")
@RestController
@RequestMapping("/payments/toss/billing")
class TossBillingController(
    private val tossBillingService: TossBillingService,
) {
    @Operation(summary = "빌링 클라이언트 설정 조회")
    @GetMapping("/client-config")
    fun clientConfig(
        @LoginUser userId: Long,
    ): ResponseEntity<TossBillingClientConfigResponse> =
        ResponseEntity.ok(tossBillingService.clientConfig(userId))

    @Operation(summary = "내 빌링 현황 조회")
    @GetMapping("/overview")
    fun overview(
        @LoginUser userId: Long,
    ): ResponseEntity<TossBillingOverviewResponse> =
        ResponseEntity.ok(tossBillingService.overview(userId))

    @Operation(summary = "빌링키 발급")
    @PostMapping("/issue")
    fun issue(
        @LoginUser userId: Long,
        @Valid @RequestBody request: IssueTossBillingKeyRequest,
    ): ResponseEntity<TossBillingMethodResponse> =
        ResponseEntity.ok(tossBillingService.issueBillingKey(userId, request))

    @Operation(summary = "기본 결제수단 변경")
    @PostMapping("/methods/{methodId}/default")
    fun setDefaultMethod(
        @LoginUser userId: Long,
        @PathVariable methodId: Long,
    ): ResponseEntity<TossBillingMethodResponse> =
        ResponseEntity.ok(tossBillingService.setDefaultMethod(userId, methodId))

    @Operation(summary = "결제수단 비활성")
    @PostMapping("/methods/{methodId}/deactivate")
    fun deactivateMethod(
        @LoginUser userId: Long,
        @PathVariable methodId: Long,
    ): ResponseEntity<TossBillingMethodResponse> =
        ResponseEntity.ok(tossBillingService.deactivateMethod(userId, methodId))

    @Operation(summary = "월간 구독 시작")
    @PostMapping("/subscriptions/start")
    fun startSubscription(
        @LoginUser userId: Long,
        @Valid @RequestBody request: StartTossBillingSubscriptionRequest,
    ): ResponseEntity<TossBillingSubscriptionResponse> =
        ResponseEntity.ok(tossBillingService.startSubscription(userId, request))

    @Operation(summary = "월간 구독 해지 예약")
    @PostMapping("/subscriptions/cancel")
    fun cancelSubscription(
        @LoginUser userId: Long,
    ): ResponseEntity<TossBillingSubscriptionResponse> =
        ResponseEntity.ok(tossBillingService.cancelSubscription(userId))

    @Operation(summary = "월간 구독 해지 철회")
    @PostMapping("/subscriptions/withdraw-cancel")
    fun withdrawCancelSubscription(
        @LoginUser userId: Long,
    ): ResponseEntity<TossBillingSubscriptionResponse> =
        ResponseEntity.ok(tossBillingService.withdrawCancelSubscription(userId))

    @Operation(summary = "실패 회차를 선택한 카드로 재결제")
    @PostMapping("/charges/{chargeId}/retry")
    fun retryCharge(
        @LoginUser userId: Long,
        @PathVariable chargeId: Long,
        @Valid @RequestBody request: RetryTossBillingChargeRequest,
    ): ResponseEntity<TossBillingChargeResponse> =
        ResponseEntity.ok(tossBillingService.retryFailedCharge(userId, chargeId, request))
}
