package com.sleekydz86.idolglow.user.user.ui.request

import com.sleekydz86.idolglow.user.user.application.dto.UpsertUserSurveyCommand
import com.sleekydz86.idolglow.user.user.domain.vo.ConceptType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class CreateUserSurveyRequest(

    @field:Schema(description = "선호 컨셉", example = "GIRL_CRUSH")
    @field:NotNull
    val concept: ConceptType,

    @field:Schema(description = "선호 아이돌 또는 그룹", example = "에스파")
    @field:NotBlank
    val idolName: String,

    @field:Schema(description = "여행 시작일", example = "2025-07-20")
    @field:NotNull
    val visitStartDate: LocalDate,

    @field:Schema(description = "여행 종료일", example = "2025-07-22")
    @field:NotNull
    val visitEndDate: LocalDate,

    @field:Schema(description = "여행 시작 시각(HH:mm)", example = "09:00")
    val visitStartTime: String? = null,

    @field:Schema(description = "여행 종료 시각(HH:mm)", example = "24:00")
    val visitEndTime: String? = null,

    @field:Schema(description = "여행 장소", example = "['용산구', '마포구']")
    val places: List<@NotBlank String>
)

fun CreateUserSurveyRequest.toCommand(): UpsertUserSurveyCommand =
    UpsertUserSurveyCommand(
        concept = concept,
        idolName = idolName,
        visitStartDate = visitStartDate,
        visitEndDate = visitEndDate,
        visitStartTime = visitStartTime?.trim()?.ifBlank { null },
        visitEndTime = visitEndTime?.trim()?.ifBlank { null },
        places = places
    )
