package com.sleekydz86.idolglow.user.user.adapter.graphql

import com.sleekydz86.idolglow.global.adapter.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.adapter.graphql.asGraphQlValue
import com.sleekydz86.idolglow.user.user.domain.dto.UserSurveyResponse
import com.sleekydz86.idolglow.user.user.domain.vo.ConceptType

data class UserSurveyGraphQlResponse(
    val id: String,
    val concept: ConceptType,
    val idolName: String,
    val visitStartDate: String,
    val visitEndDate: String,
    val places: List<String>,
) {
    companion object {
        fun from(response: UserSurveyResponse): UserSurveyGraphQlResponse =
            UserSurveyGraphQlResponse(
                id = response.id.asGraphQlId(),
                concept = response.concept,
                idolName = response.idolName,
                visitStartDate = requireNotNull(response.visitStartDate.asGraphQlValue()),
                visitEndDate = requireNotNull(response.visitEndDate.asGraphQlValue()),
                places = response.places,
            )
    }
}
