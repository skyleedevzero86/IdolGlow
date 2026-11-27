package com.sleekydz86.idolglow.user.auth.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.oauth2.kakao")
data class KakaoOAuthProperties(
    val clientId: String = "",
    val clientSecret: String = "",
) {
    val configured: Boolean
        get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    override fun toString(): String = "KakaoOAuthProperties(configured=$configured)"
}
