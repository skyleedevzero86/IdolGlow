package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.payment.application.TossBillingService
import com.sleekydz86.idolglow.payment.application.dto.ChargeTossBillingRequest
import com.sleekydz86.idolglow.payment.application.dto.TossBillingChargeResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("local", "test")
@Tag(name = "토스 빌링", description = "카드 자동결제(빌링키) 등록·구독·승인")
@RestController
@RequestMapping("/payments/toss/billing")
class TossBillingOneOffChargeController(
    private val tossBillingService: TossBillingService,
) {
    @Operation(summary = "빌링키로 즉시 결제(테스트용)")
    @PostMapping("/charge")
    fun charge(
        @LoginUser userId: Long,
        @Valid @RequestBody request: ChargeTossBillingRequest,
    ): ResponseEntity<TossBillingChargeResponse> =
        ResponseEntity.ok(tossBillingService.chargeNow(userId, request))
}
