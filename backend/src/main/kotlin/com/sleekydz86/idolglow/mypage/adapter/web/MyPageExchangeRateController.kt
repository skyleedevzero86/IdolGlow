package com.sleekydz86.idolglow.mypage.ui

import com.sleekydz86.idolglow.exchangerate.adapter.web.dto.ExchangeRateItemResponse
import com.sleekydz86.idolglow.exchangerate.application.ExchangeRateQueryService
import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "마이페이지-환율")
@RestController
@RequestMapping("/mypage")
class MyPageExchangeRateController(
    private val exchangeRateQueryService: ExchangeRateQueryService,
) {

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
