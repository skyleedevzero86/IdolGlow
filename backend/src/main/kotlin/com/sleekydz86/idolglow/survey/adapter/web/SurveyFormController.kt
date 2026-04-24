package com.sleekydz86.idolglow.survey.ui

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.survey.application.UserSurveyFormService
import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveySubmissionResponse
import com.sleekydz86.idolglow.survey.ui.request.SubmitSurveyResponseRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/survey-forms")
class SurveyFormController(
    private val userSurveyFormService: UserSurveyFormService,
) {
    @GetMapping("/current")
    fun findCurrent(): ResponseEntity<SurveyFormResponse> =
        userSurveyFormService.findCurrentForm()
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.noContent().build()

    @PostMapping("/current/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    fun submitCurrent(
        @LoginUser userId: Long,
        @Valid @RequestBody request: SubmitSurveyResponseRequest,
    ): ResponseEntity<SurveySubmissionResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(userSurveyFormService.submitCurrentForm(userId, request))

    @GetMapping("/current/submissions/me/latest")
    fun findMyLatest(@LoginUser userId: Long): ResponseEntity<SurveySubmissionResponse> =
        userSurveyFormService.findMyLatestSubmission(userId)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.noContent().build()
}
