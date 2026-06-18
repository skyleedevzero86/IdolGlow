package com.sleekydz86.idolglow.mypage.adapter.web

import com.sleekydz86.idolglow.exchangerate.adapter.web.dto.ExchangeRateItemResponse
import com.sleekydz86.idolglow.exchangerate.application.ExchangeRateQueryService
import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "Glow 환율", description = "마이페이지 일별 환율 조회 API")
@RestController
@RequestMapping("/mypage")
class MyPageExchangeRateController(
    private val exchangeRateQueryService: ExchangeRateQueryService,
) {
    @Operation(
        summary = "일별 환율 조회",
        description = "searchDate 미입력 시 당일 기준 주요 통화 환율 목록을 반환합니다.",
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/exchange-rates/daily")
    fun daily(
        @LoginUser userId: Long,
        @RequestParam(name = "searchDate", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        searchDate: LocalDate?,
    ): List<ExchangeRateItemResponse> {
        check(userId > 0L)
        return exchangeRateQueryService.getDailyRates(searchDate).map(ExchangeRateItemResponse::from)
    }
}
