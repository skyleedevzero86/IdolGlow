package com.sleekydz86.idolglow.exchange.adapter.web

import com.sleekydz86.idolglow.exchange.adapter.web.dto.CreateExchangeAlertRequest
import com.sleekydz86.idolglow.exchange.adapter.web.dto.ExchangeAlertCreatedResponse
import com.sleekydz86.idolglow.exchange.adapter.web.dto.toCommand
import com.sleekydz86.idolglow.exchange.application.ExchangeAlertCommandService
import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Glow 환전", description = "환전 알림 등록 API")
@Validated
@RestController
class ExchangeAlertController(
    private val exchangeAlertCommandService: ExchangeAlertCommandService,
) {
    @Operation(
        summary = "환전 알림 등록",
        description = "목표 환율·통화 조건으로 환전 알림을 생성합니다. `/exchange-alerts`, `/mypage/exchange-alerts`, `/api/exchange-alerts` 경로를 모두 지원합니다.",
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/exchange-alerts", "/mypage/exchange-alerts", "/api/exchange-alerts")
    fun create(
        @LoginUser userId: Long,
        @Valid @RequestBody body: CreateExchangeAlertRequest,
    ): ExchangeAlertCreatedResponse {
        check(userId > 0L)
        val id = exchangeAlertCommandService.create(userId, body.toCommand())
        return ExchangeAlertCreatedResponse(id)
    }
}
