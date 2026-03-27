package com.sleekydz86.idolglow.user.auth.oauth

import com.sleekydz86.idolglow.user.auth.application.userInfo.OAuth2UserInfo
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import org.springframework.security.oauth2.core.user.OAuth2User

class CustomOAuth2User(
    val provider: AuthProvider,
    val userInfo: OAuth2UserInfo,
    private val delegate: OAuth2User
) : OAuth2User {

    override fun getAttributes(): Map<String, Any> = delegate.attributes

    override fun getAuthorities() = delegate.authorities

    override fun getName(): String = userInfo.id
}
