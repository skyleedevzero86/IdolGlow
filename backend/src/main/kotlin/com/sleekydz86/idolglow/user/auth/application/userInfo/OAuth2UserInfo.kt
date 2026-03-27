package com.sleekydz86.idolglow.user.auth.application.userInfo

import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider

interface OAuth2UserInfo {
    val id : String
    val email: String
    val name: String?
    val picture: String?

    companion object {
        fun of(provider: AuthProvider, attributes: Map<String, Any>): OAuth2UserInfo =
            when (provider) {
                AuthProvider.GOOGLE -> GoogleUserInfo(attributes)
                else -> throw IllegalArgumentException("지원하지 않는 로그인 제공자입니다: $provider")
            }
    }
}
