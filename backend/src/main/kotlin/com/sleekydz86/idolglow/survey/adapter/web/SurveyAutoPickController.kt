package com.sleekydz86.idolglow.survey.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.survey.application.autopick.SurveyAutoPickCandidateResponse
import com.sleekydz86.idolglow.survey.application.autopick.SurveyAutoPickCommand
import com.sleekydz86.idolglow.survey.application.autopick.SurveyAutoPickResponse
import com.sleekydz86.idolglow.survey.application.autopick.SurveyAutoPickSettingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@Tag(name = "설문 추천 자동 담기", description = "추천 상품 슬롯이 회원 오픈되면 기본 카드로 자동 예약")
@RestController
@RequestMapping("/survey-auto-pick")
class SurveyAutoPickController(
    private val settingService: SurveyAutoPickSettingService,
) {
    @Operation(summary = "자동 담기 설정과 최근 결제 예정 내역 조회")
    @GetMapping
    fun find(
        @LoginUser userId: Long,
    ): ResponseEntity<SurveyAutoPickResponse> = ResponseEntity.ok(settingService.find(userId))

    @Operation(summary = "자동 담기 설정 저장")
    @PutMapping
    fun update(
        @LoginUser userId: Long,
        @Valid @RequestBody request: SurveyAutoPickRequest,
    ): ResponseEntity<SurveyAutoPickResponse> = ResponseEntity.ok(settingService.update(userId, request.toCommand()))

    @Operation(summary = "자동 담기 결제 예정 건너뛰기")
    @PostMapping("/candidates/{candidateId}/skip")
    fun skip(
        @LoginUser userId: Long,
        @PathVariable candidateId: Long,
    ): ResponseEntity<SurveyAutoPickCandidateResponse> = ResponseEntity.ok(settingService.skip(userId, candidateId))
}

@Schema(description = "자동 담기 설정 요청")
data class SurveyAutoPickRequest(
    @field:Schema(description = "자동 담기 켜짐 여부")
    val enabled: Boolean,
    @field:NotNull
    @field:Schema(description = "대상 추천 상품 번호. 1개 이상 10개 이하")
    val productIds: List<Long>?,
    @field:NotNull
    @field:Schema(description = "예산 상한 원 단위. 0 이상")
    val maxPrice: BigDecimal?,
) {
    fun toCommand(): SurveyAutoPickCommand =
        SurveyAutoPickCommand(enabled, productIds.orEmpty(), maxPrice ?: BigDecimal.ZERO)
}
