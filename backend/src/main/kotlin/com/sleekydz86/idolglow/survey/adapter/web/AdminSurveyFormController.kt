package com.sleekydz86.idolglow.survey.adapter.web

import com.sleekydz86.idolglow.survey.adapter.web.request.AdminUpsertSurveyFormRequest
import com.sleekydz86.idolglow.survey.adapter.web.request.toCommand
import com.sleekydz86.idolglow.survey.application.AdminSurveyFormService
import com.sleekydz86.idolglow.survey.application.SurveyFormStatisticsService
import com.sleekydz86.idolglow.survey.domain.SurveyFormPrimaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormSecondaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormStatus
import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormPageResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormStatisticsResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormSummaryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 설문", description = "관리자 설문지 CRUD API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/survey-forms")
class AdminSurveyFormController(
    private val adminSurveyFormService: AdminSurveyFormService,
    private val surveyFormStatisticsService: SurveyFormStatisticsService,
) {
    @Operation(summary = "설문 목록 조회")
    @GetMapping
    fun list(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: SurveyFormStatus?,
        @RequestParam(required = false) primaryCategory: SurveyFormPrimaryCategory?,
        @RequestParam(required = false) secondaryCategory: SurveyFormSecondaryCategory?,
    ): List<SurveyFormSummaryResponse> = adminSurveyFormService.list(keyword, status, primaryCategory, secondaryCategory)

    @Operation(summary = "설문 페이지 조회")
    @GetMapping("/page")
    fun listPage(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: SurveyFormStatus?,
        @RequestParam(required = false) primaryCategory: SurveyFormPrimaryCategory?,
        @RequestParam(required = false) secondaryCategory: SurveyFormSecondaryCategory?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
    ): SurveyFormPageResponse = adminSurveyFormService.listPage(keyword, status, primaryCategory, secondaryCategory, page, size)

    @Operation(summary = "현재 설문 조회")
    @GetMapping("/current")
    fun findCurrent(): ResponseEntity<SurveyFormResponse> =
        adminSurveyFormService
            .findCurrent()
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.noContent().build()

    @Operation(summary = "설문 상세 조회")
    @GetMapping("/{id}")
    fun find(
        @PathVariable id: Long,
    ): SurveyFormResponse = adminSurveyFormService.find(id)

    @Operation(summary = "설문 문항별 응답 통계")
    @GetMapping("/{id}/statistics")
    fun statistics(
        @PathVariable id: Long,
    ): SurveyFormStatisticsResponse = surveyFormStatisticsService.statistics(id)

    @Operation(summary = "설문 통계 엑셀 다운로드")
    @GetMapping("/{id}/statistics.xlsx")
    fun statisticsXlsx(
        @PathVariable id: Long,
    ): ResponseEntity<ByteArrayResource> {
        val bytes = surveyFormStatisticsService.exportXlsx(id)
        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=survey-$id-statistics.xlsx")
            .body(ByteArrayResource(bytes))
    }

    @Operation(summary = "설문 생성")
    @PostMapping
    fun create(
        @Valid @RequestBody request: AdminUpsertSurveyFormRequest,
    ): ResponseEntity<SurveyFormResponse> = ResponseEntity.ok(adminSurveyFormService.create(request.toCommand()))

    @Operation(summary = "설문 수정")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminUpsertSurveyFormRequest,
    ): ResponseEntity<SurveyFormResponse> = ResponseEntity.ok(adminSurveyFormService.update(id, request.toCommand()))

    @Operation(summary = "현재 설문 등록/수정")
    @PutMapping("/current")
    fun upsertCurrent(
        @Valid @RequestBody request: AdminUpsertSurveyFormRequest,
    ): ResponseEntity<SurveyFormResponse> = ResponseEntity.ok(adminSurveyFormService.upsertCurrent(request.toCommand()))
}
