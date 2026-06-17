package com.sleekydz86.idolglow.user.user.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlValue
import com.sleekydz86.idolglow.user.user.application.dto.GetUserLoginInfoResponse
import com.sleekydz86.idolglow.user.user.domain.dto.UserSurveyResponse
import com.sleekydz86.idolglow.user.user.domain.dto.UserSurveyUpsertResponse
import com.sleekydz86.idolglow.user.user.domain.vo.ConceptType

data class UserSurveyUpsertGraphQlResponse(
    val id: String,
) {
    companion object {
        fun from(response: UserSurveyUpsertResponse): UserSurveyUpsertGraphQlResponse =
            UserSurveyUpsertGraphQlResponse(id = response.id.asGraphQlId())
    }
}
