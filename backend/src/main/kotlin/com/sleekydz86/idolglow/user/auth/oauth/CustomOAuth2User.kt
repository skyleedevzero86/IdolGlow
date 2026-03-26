package com.sleekydz86.idolglow.user.auth.oauth

import org.springframework.security.oauth2.core.user.OAuth2User
import java.security.AuthProvider

class CustomOAuth2User(
    val provider: AuthProvider,
    val userInfo: OAuth2UserInfo,
    private val delegate: OAuth2User
) : OAuth2User {

    override fun getAttributes(): Map<String, Any> = delegate.attributes

    override fun getAuthorities() = delegate.authorities

    override fun getName(): String = userInfo.id
}
