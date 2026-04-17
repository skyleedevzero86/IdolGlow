package com.sleekydz86.idolglow.user.user.ui

import com.sleekydz86.idolglow.user.user.domain.dto.UserSurveyResponse
import com.sleekydz86.idolglow.user.user.domain.dto.UserSurveyUpsertResponse
import com.sleekydz86.idolglow.user.user.ui.request.CreateUserSurveyRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity

@Tag(name = "회원 설문", description = "회원 설문 관련 API")
interface UserSurveyApi {

    @Operation(
        summary = "설문 조회",
        description = "설문 내용을 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "404", description = "설문을 찾을 수 없음")
        ]
    )
    fun findUserSurvey(
        @Parameter(hidden = true)
        userId: Long,
    ): ResponseEntity<UserSurveyResponse>

    @Operation(
        summary = "설문 저장",
        description = "설문 내용을 저장합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "생성 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청"),
            ApiResponse(responseCode = "409", description = "이미 설문이 존재함")
        ]
    )
    fun upsertUserSurvey(
        @Parameter(hidden = true)
        userId: Long,
        @Valid request: CreateUserSurveyRequest
    ): ResponseEntity<UserSurveyUpsertResponse>
}
