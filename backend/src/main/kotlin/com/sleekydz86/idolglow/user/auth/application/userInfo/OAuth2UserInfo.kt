package com.sleekydz86.idolglow.user.auth.application.userInfo

import java.security.AuthProvider

interface OAuth2UserInfo {
    val id : String
    val email: String
    val name: String?

    companion object {
        fun of(provider: AuthProvider, attributes: Map<String, Any>): OAuth2UserInfo =
            when (provider) {
                AuthProvider.GOOGLE -> GoogleUserInfo(attributes)
                else -> throw IllegalArgumentException("Unsupported provider: $provider")
            }
    }
}