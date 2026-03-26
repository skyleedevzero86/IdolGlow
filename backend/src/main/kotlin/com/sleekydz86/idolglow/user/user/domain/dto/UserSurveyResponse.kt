package com.sleekydz86.idolglow.user.user.domain.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class UserSurveyResponse(
    @field:Schema(description = "설문조사 ID", example = "1")
    val id: Long,

    @field:Schema(description = "선호 컨셉", example = "GIRL_CRUSH")
    val concept: ConceptType,

    @field:Schema(description = "선호 아이돌 또는 그룹", example = "에스파")
    val idolName: String,

    @field:Schema(description = "여행 시작일", example = "2025-07-20")
    val visitStartDate: LocalDate,

    @field:Schema(description = "여행 종료일", example = "2025-07-22")
    val visitEndDate: LocalDate,

    @field:Schema(description = "여행 장소", example = "['용산구', '마포구']")
    val places: List<String>
) {
    companion object {
        fun from(userSurvey: UserSurvey): UserSurveyResponse {
            return UserSurveyResponse(
                id = userSurvey.id,
                concept = userSurvey.concept,
                idolName = userSurvey.idolName,
                visitStartDate = userSurvey.visitStartDate,
                visitEndDate = userSurvey.visitEndDate,
                places = userSurvey.places
            )
        }
    }
}
