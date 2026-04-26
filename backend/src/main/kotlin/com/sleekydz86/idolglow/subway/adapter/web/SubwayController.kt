package com.sleekydz86.idolglow.subway.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.subway.adapter.web.dto.SubwayLineDto
import com.sleekydz86.idolglow.subway.adapter.web.dto.SubwayPageDto
import com.sleekydz86.idolglow.subway.adapter.web.dto.SubwayStationDto
import com.sleekydz86.idolglow.subway.adapter.web.dto.SubwayStationRefDto
import com.sleekydz86.idolglow.subway.application.port.incoming.SubwayQueryUseCase
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Glow 지하철")
@RestController
@RequestMapping("/mypage/subway", "/api/subway")
class SubwayController(
    private val subwayQueryUseCase: SubwayQueryUseCase,
    private val subwayWebMapper: SubwayWebMapper,
) {

    @GetMapping("/lines")
    fun lines(@LoginUser userId: Long): List<SubwayLineDto> {
        check(userId > 0L) { "로그인이 필요합니다." }
        return subwayQueryUseCase.listLines().map(subwayWebMapper::toLineDto)
    }

    @GetMapping("/lines/{lineId}/stations")
    fun stations(
        @LoginUser userId: Long,
        @PathVariable lineId: String,
    ): List<SubwayStationDto> {
        check(userId > 0L) { "로그인이 필요합니다." }
        return subwayQueryUseCase.listStations(lineId).map(subwayWebMapper::toStationDto)
    }

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
