package com.sleekydz86.idolglow.user.user.adapter.graphql

import com.sleekydz86.idolglow.global.adapter.graphql.asGraphQlId
import com.sleekydz86.idolglow.user.user.domain.dto.UserSurveyUpsertResponse

data class UserSurveyUpsertGraphQlResponse(
    val id: String,
) {
    companion object {
        fun from(response: UserSurveyUpsertResponse): UserSurveyUpsertGraphQlResponse =
            UserSurveyUpsertGraphQlResponse(id = response.id.asGraphQlId())
    }
}
