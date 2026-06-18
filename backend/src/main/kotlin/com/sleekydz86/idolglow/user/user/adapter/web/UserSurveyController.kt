package com.sleekydz86.idolglow.user.user.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.user.user.adapter.web.request.CreateUserSurveyRequest
import com.sleekydz86.idolglow.user.user.adapter.web.request.toCommand
import com.sleekydz86.idolglow.user.user.application.UserSurveyCommandService
import com.sleekydz86.idolglow.user.user.application.UserSurveyQueryService
import com.sleekydz86.idolglow.user.user.domain.dto.UserSurveyResponse
import com.sleekydz86.idolglow.user.user.domain.dto.UserSurveyUpsertResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RequestMapping("/surveys")
@RestController
class UserSurveyController(
    private val userSurveyCommandService: UserSurveyCommandService,
    private val userSurveyQueryService: UserSurveyQueryService,
) : UserSurveyApi {
    @GetMapping
    override fun findUserSurvey(
        @LoginUser userId: Long,
    ): ResponseEntity<UserSurveyResponse> {
        val body =
            userSurveyQueryService.findUserSurveyIfPresent(userId)
                ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(body)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun upsertUserSurvey(
        @LoginUser userId: Long,
        @Valid @RequestBody request: CreateUserSurveyRequest,
    ): ResponseEntity<UserSurveyUpsertResponse> {
        val survey =
            userSurveyCommandService.saveUserSurvey(
                userId,
                request.toCommand(),
            )
        return ResponseEntity
            .created(URI.create("/surveys"))
            .body(UserSurveyUpsertResponse(id = survey.id))
    }

    @DeleteMapping("/places")
    override fun clearUserSurveyPlaces(
        @LoginUser userId: Long,
    ): ResponseEntity<Void> {
        userSurveyCommandService.clearUserSurveyPlaces(userId)
        return ResponseEntity.noContent().build()
    }
}
