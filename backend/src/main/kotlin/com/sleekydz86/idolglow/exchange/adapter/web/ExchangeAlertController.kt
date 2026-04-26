package com.sleekydz86.idolglow.exchange.adapter.web

import com.sleekydz86.idolglow.exchange.adapter.web.dto.CreateExchangeAlertRequest
import com.sleekydz86.idolglow.exchange.adapter.web.dto.ExchangeAlertCreatedResponse
import com.sleekydz86.idolglow.exchange.application.ExchangeAlertCommandService
import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "환전")
@Validated
@RestController
class ExchangeAlertController(
    private val exchangeAlertCommandService: ExchangeAlertCommandService,
) {

    @PostMapping("/exchange-alerts")
    fun create(
        @LoginUser userId: Long,
        @Valid @RequestBody body: CreateExchangeAlertRequest,
    ): ExchangeAlertCreatedResponse {
        check(userId > 0L)
        val id = exchangeAlertCommandService.create(userId, body)
        return ExchangeAlertCreatedResponse(id)
    }
}
