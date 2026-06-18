package com.sleekydz86.idolglow.survey.ui

import com.sleekydz86.idolglow.survey.application.AdminSurveyFormService
import com.sleekydz86.idolglow.survey.domain.SurveyFormPrimaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormSecondaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormStatus
import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormPageResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormSummaryResponse
import com.sleekydz86.idolglow.survey.ui.request.AdminUpsertSurveyFormRequest
import jakarta.validation.Valid
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

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/survey-forms")
class AdminSurveyFormController(
    private val adminSurveyFormService: AdminSurveyFormService,
) {
    @GetMapping
    fun list(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: SurveyFormStatus?,
        @RequestParam(required = false) primaryCategory: SurveyFormPrimaryCategory?,
        @RequestParam(required = false) secondaryCategory: SurveyFormSecondaryCategory?,
    ): List<SurveyFormSummaryResponse> = adminSurveyFormService.list(keyword, status, primaryCategory, secondaryCategory)

    @GetMapping("/page")
    fun listPage(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: SurveyFormStatus?,
        @RequestParam(required = false) primaryCategory: SurveyFormPrimaryCategory?,
        @RequestParam(required = false) secondaryCategory: SurveyFormSecondaryCategory?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
    ): SurveyFormPageResponse = adminSurveyFormService.listPage(keyword, status, primaryCategory, secondaryCategory, page, size)

    @GetMapping("/current")
    fun findCurrent(): ResponseEntity<SurveyFormResponse> =
        adminSurveyFormService
            .findCurrent()
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.noContent().build()

    @GetMapping("/{id}")
    fun find(
        @PathVariable id: Long,
    ): SurveyFormResponse = adminSurveyFormService.find(id)

    @PostMapping
    fun create(
        @Valid @RequestBody request: AdminUpsertSurveyFormRequest,
    ): ResponseEntity<SurveyFormResponse> = ResponseEntity.ok(adminSurveyFormService.create(request))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminUpsertSurveyFormRequest,
    ): ResponseEntity<SurveyFormResponse> = ResponseEntity.ok(adminSurveyFormService.update(id, request))

    @PutMapping("/current")
    fun upsertCurrent(
        @Valid @RequestBody request: AdminUpsertSurveyFormRequest,
    ): ResponseEntity<SurveyFormResponse> = ResponseEntity.ok(adminSurveyFormService.upsertCurrent(request))
}
