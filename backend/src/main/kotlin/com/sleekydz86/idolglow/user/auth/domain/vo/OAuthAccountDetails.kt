package com.sleekydz86.idolglow.user.auth.domain.vo

data class OAuthAccountDetails(
    val accountName: String?,
    val gender: String?,
    val ageRange: String?,
) {
    override fun toString(): String = "OAuthAccountDetails(hasName=${accountName != null})"
}
