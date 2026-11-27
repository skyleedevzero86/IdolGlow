package com.sleekydz86.idolglow.luckydraw.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.luckydraw.application.LuckyDrawEntryService
import com.sleekydz86.idolglow.luckydraw.application.LuckyDrawQueryService
import com.sleekydz86.idolglow.luckydraw.application.dto.LuckyDrawMyStatusResponse
import com.sleekydz86.idolglow.luckydraw.application.dto.LuckyDrawSummaryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "응모", description = "예약 판매 화면의 응모 행사 API")
@RestController
@RequestMapping("/lucky-draws")
class LuckyDrawController(
    private val luckyDrawQueryService: LuckyDrawQueryService,
    private val luckyDrawEntryService: LuckyDrawEntryService,
) {
    @Operation(summary = "현재 응모 행사 조회", description = "응모 중 → 마감 → 예정 → 최근 추첨 순으로 하나를 돌려줍니다. 없으면 204입니다.")
    @GetMapping("/current")
    fun current(): ResponseEntity<LuckyDrawSummaryResponse> =
        luckyDrawQueryService.current()?.let { ResponseEntity.ok(it) } ?: ResponseEntity.noContent().build()

    @Operation(summary = "내 응모 현황 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{drawId}/me")
    fun me(
        @LoginUser userId: Long,
        @PathVariable drawId: Long,
    ): ResponseEntity<LuckyDrawMyStatusResponse> = ResponseEntity.ok(luckyDrawEntryService.myStatus(drawId, userId))

    @Operation(summary = "응모하기", description = "응모 기간 안에 하루 한 번 응모합니다. 같은 날 두 번째는 409입니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{drawId}/entries")
    fun enter(
        @LoginUser userId: Long,
        @PathVariable drawId: Long,
    ): ResponseEntity<LuckyDrawMyStatusResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(luckyDrawEntryService.enter(drawId, userId))
}
