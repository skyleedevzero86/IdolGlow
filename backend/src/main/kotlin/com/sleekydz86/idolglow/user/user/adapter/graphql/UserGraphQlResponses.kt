package com.sleekydz86.idolglow.user.user.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlValue
import com.sleekydz86.idolglow.user.user.application.dto.GetUserLoginInfoResponse
import com.sleekydz86.idolglow.user.user.domain.dto.UserSurveyResponse
import com.sleekydz86.idolglow.user.user.domain.dto.UserSurveyUpsertResponse
import com.sleekydz86.idolglow.user.user.domain.vo.ConceptType

data class UserProfileGraphQlResponse(
    val id: String,
    val email: String,
    val nickname: String,
    val name: String?,
    val picture: String?,
    val oauthLinked: Boolean,
    val hasPassword: Boolean,
    val role: String,
    val lastLoginAt: String?,
) {
    companion object {
        fun from(response: GetUserLoginInfoResponse): UserProfileGraphQlResponse =
            UserProfileGraphQlResponse(
                id = response.id.asGraphQlId(),
                email = response.email,
                nickname = response.nickname,
                name = response.name,
                picture = response.picture,
                oauthLinked = response.oauthLinked,
                hasPassword = response.hasPassword,
                role = response.role,
                lastLoginAt = response.lastLoginAt.asGraphQlValue()
            )
    }
}

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
                places = response.places
            )
    }
}

data class UserSurveyUpsertGraphQlResponse(
    val id: String,
) {
    companion object {
        fun from(response: UserSurveyUpsertResponse): UserSurveyUpsertGraphQlResponse =
            UserSurveyUpsertGraphQlResponse(id = response.id.asGraphQlId())
    }
}
