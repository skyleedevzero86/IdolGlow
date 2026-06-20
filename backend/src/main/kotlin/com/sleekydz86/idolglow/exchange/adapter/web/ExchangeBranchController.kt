package com.sleekydz86.idolglow.exchange.adapter.web

import com.sleekydz86.idolglow.exchange.adapter.web.dto.ExchangeBranchResponse
import com.sleekydz86.idolglow.exchange.adapter.web.dto.toWebResponse
import com.sleekydz86.idolglow.exchange.application.ExchangeBranchQueryService
import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Glow 환전", description = "환전소·지점 조회 API")
@RestController
@RequestMapping("/exchange", "/mypage/exchange", "/api/exchange")
class ExchangeBranchController(
    private val exchangeBranchQueryService: ExchangeBranchQueryService,
) {
    @Operation(
        summary = "환전 지점 목록 조회",
        description = "통화 코드 기준 환전 지점 목록과 예상 이동 시간(분)을 반환합니다.",
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/branches")
    fun branches(
        @LoginUser userId: Long,
        @RequestParam currency: String,
    ): List<ExchangeBranchResponse> {
        check(userId > 0L)
        return exchangeBranchQueryService.listBranchesWithDrivingMinutes(currency).map { it.toWebResponse() }
    }
}
