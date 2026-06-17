package com.sleekydz86.idolglow.user.auth.graphql

import com.sleekydz86.idolglow.user.auth.application.SignupFieldCheckResult
import com.sleekydz86.idolglow.user.auth.application.dto.SignupCheckResponse
import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse

data class SignupCheckGraphQlResponse(
    val available: Boolean,
    val code: String?,
) {
    companion object {
        fun from(response: SignupCheckResponse): SignupCheckGraphQlResponse =
            SignupCheckGraphQlResponse(
                available = response.available,
                code = response.code
            )

        fun from(response: SignupFieldCheckResult): SignupCheckGraphQlResponse =
            SignupCheckGraphQlResponse(
                available = response.available,
                code = response.code
            )
    }
}
