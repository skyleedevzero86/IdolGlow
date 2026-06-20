package com.sleekydz86.idolglow.user.user.adapter.graphql

import com.sleekydz86.idolglow.global.adapter.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.adapter.graphql.asGraphQlValue
import com.sleekydz86.idolglow.user.user.application.dto.GetUserLoginInfoResponse

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
                lastLoginAt = response.lastLoginAt.asGraphQlValue(),
            )
    }
}
