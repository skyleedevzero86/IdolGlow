package com.sleekydz86.idolglow.user.auth.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.oauth2.naver")
data class NaverOAuthProperties(
    val clientId: String = "",
    val clientSecret: String = "",
) {
    val configured: Boolean
        get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    override fun toString(): String = "NaverOAuthProperties(configured=$configured)"
}
