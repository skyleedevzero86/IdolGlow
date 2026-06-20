package com.sleekydz86.idolglow.glowweather.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.glowweather.application.GlowWeatherDashboardResponse
import com.sleekydz86.idolglow.glowweather.application.GlowWeatherQueryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Glow 날씨", description = "KMA 기반 마이페이지 날씨 대시보드 API")
@RestController
@RequestMapping("/mypage/glow-weather")
class GlowWeatherController(
    private val glowWeatherQueryService: GlowWeatherQueryService,
) {
    @Operation(summary = "Glow 날씨 대시보드 조회", description = "regionId 미입력 시 기본 지역 날씨·예보를 반환합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/dashboard")
    fun dashboard(
        @LoginUser userId: Long,
        @RequestParam(required = false) regionId: String?,
    ): GlowWeatherDashboardResponse {
        check(userId > 0L)
        return glowWeatherQueryService.dashboard(regionId)
    }
}
