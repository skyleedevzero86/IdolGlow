package com.sleekydz86.idolglow.exchange.adapter.web

import com.sleekydz86.idolglow.exchange.adapter.web.dto.ExchangeBranchResponse
import com.sleekydz86.idolglow.exchange.application.ExchangeBranchQueryService
import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "환전")
@RestController
@RequestMapping("/exchange")
class ExchangeBranchController(
    private val exchangeBranchQueryService: ExchangeBranchQueryService,
) {

    @GetMapping("/branches")
    fun branches(
        @LoginUser userId: Long,
        @RequestParam currency: String,
    ): List<ExchangeBranchResponse> {
        check(userId > 0L)
        return exchangeBranchQueryService.listBranchesWithDrivingMinutes(currency)
    }
}
