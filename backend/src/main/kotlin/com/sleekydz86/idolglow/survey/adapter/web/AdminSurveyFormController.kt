package com.sleekydz86.idolglow.survey.ui

import com.sleekydz86.idolglow.survey.application.AdminSurveyFormService
import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormResponse
import com.sleekydz86.idolglow.survey.ui.request.AdminUpsertSurveyFormRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/survey-forms")
class AdminSurveyFormController(
    private val adminSurveyFormService: AdminSurveyFormService,
) {
    @GetMapping("/current")
    fun findCurrent(): ResponseEntity<SurveyFormResponse> =
        adminSurveyFormService.findCurrent()
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.noContent().build()

    @PutMapping("/current")
    fun upsertCurrent(
        @Valid @RequestBody request: AdminUpsertSurveyFormRequest,
    ): ResponseEntity<SurveyFormResponse> =
        ResponseEntity.ok(adminSurveyFormService.upsertCurrent(request))
}
