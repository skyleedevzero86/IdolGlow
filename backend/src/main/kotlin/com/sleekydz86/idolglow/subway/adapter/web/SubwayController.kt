package com.sleekydz86.idolglow.subway.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.subway.adapter.web.dto.SubwayLineDto
import com.sleekydz86.idolglow.subway.adapter.web.dto.SubwayPageDto
import com.sleekydz86.idolglow.subway.adapter.web.dto.SubwayStationDto
import com.sleekydz86.idolglow.subway.adapter.web.dto.SubwayStationRefDto
import com.sleekydz86.idolglow.subway.application.port.incoming.SubwayQueryUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Glow 지하철", description = "수도권 지하철 노선·역 조회 API")
@RestController
@RequestMapping("/mypage/subway", "/api/subway")
class SubwayController(
    private val subwayQueryUseCase: SubwayQueryUseCase,
    private val subwayWebMapper: SubwayWebMapper,
) {
    @Operation(summary = "지하철 노선 목록 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/lines")
    fun lines(
        @LoginUser userId: Long,
    ): List<SubwayLineDto> {
        check(userId > 0L) { "로그인이 필요합니다." }
        return subwayQueryUseCase.listLines().map(subwayWebMapper::toLineDto)
    }

    @Operation(summary = "노선별 역 목록 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/lines/{lineId}/stations")
    fun stations(
        @LoginUser userId: Long,
        @PathVariable lineId: String,
    ): List<SubwayStationDto> {
        check(userId > 0L) { "로그인이 필요합니다." }
        return subwayQueryUseCase.listStations(lineId).map(subwayWebMapper::toStationDto)
    }

    @Operation(summary = "역 검색", description = "키워드(q)로 역명·노선을 검색합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/stations/search")
    fun search(
        @LoginUser userId: Long,
        @RequestParam(name = "q", defaultValue = "") q: String,
    ): List<SubwayStationRefDto> {
        check(userId > 0L) { "로그인이 필요합니다." }
        return subwayQueryUseCase.searchStations(q).map { on ->
            subwayWebMapper.toRefDto(on.line, on.stop)
        }
    }

    @Operation(summary = "역 상세 페이지 조회", description = "노선·역 코드 기준 역 상세(환승·인접역 등)를 반환합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/lines/{lineId}/stations/{stationCd}/page")
    fun stationPage(
        @LoginUser userId: Long,
        @PathVariable lineId: String,
        @PathVariable stationCd: String,
    ): SubwayPageDto {
        check(userId > 0L) { "로그인이 필요합니다." }
        return subwayWebMapper.toPageDto(subwayQueryUseCase.getStationPage(lineId, stationCd))
    }
}
