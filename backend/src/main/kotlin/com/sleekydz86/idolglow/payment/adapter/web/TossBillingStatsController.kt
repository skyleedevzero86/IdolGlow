package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.payment.application.MyGlowPassStatsService
import com.sleekydz86.idolglow.payment.application.dto.MyGlowPassStatsResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "토스 빌링 이용 통계", description = "내 Glow Pass 결제·혜택 이용 통계")
@RestController
@RequestMapping("/payments/toss/billing")
class TossBillingStatsController(
    private val myGlowPassStatsService: MyGlowPassStatsService,
) {
    @Operation(summary = "내 Glow Pass 이용 통계 조회")
    @GetMapping("/my-stats")
    fun myStats(
        @LoginUser userId: Long,
    ): ResponseEntity<MyGlowPassStatsResponse> = ResponseEntity.ok(myGlowPassStatsService.stats(userId))
}
