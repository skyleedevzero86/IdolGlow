package com.sleekydz86.idolglow.global.config

import com.sleekydz86.idolglow.user.auth.application.KakaoOAuthProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties
import org.springframework.security.oauth2.core.ClientAuthenticationMethod

@DisplayName("OAuth 등록 정보")
class OAuth2ClientRegistrationConfigTest {
    private val config = OAuth2ClientRegistrationConfig()

    @DisplayName("결정 1 카카오 키가 둘 다 있으면 카카오 등록을 더하고 구글 등록은 그대로 둔다")
    @Test
    fun addsKakaoWhenConfigured() {
        // when
        val repository = config.clientRegistrationRepository(googleProperties(), KakaoOAuthProperties("rest-key", "secret"))

        // then
        val kakao = repository.findByRegistrationId("kakao")
        assertNotNull(repository.findByRegistrationId("google"))
        assertNotNull(kakao)
        assertEquals("https://kauth.kakao.com/oauth/authorize", kakao!!.providerDetails.authorizationUri)
        assertEquals("https://kauth.kakao.com/oauth/token", kakao.providerDetails.tokenUri)
        assertEquals("https://kapi.kakao.com/v2/user/me", kakao.providerDetails.userInfoEndpoint.uri)
        assertEquals("id", kakao.providerDetails.userInfoEndpoint.userNameAttributeName)
        assertEquals(ClientAuthenticationMethod.CLIENT_SECRET_POST, kakao.clientAuthenticationMethod)
        assertEquals(setOf("profile_nickname", "profile_image", "account_email"), kakao.scopes)
    }

    @DisplayName("R13 카카오 키가 하나라도 비면 카카오 등록 없이 앱 설정이 만들어진다")
    @Test
    fun skipsKakaoWhenAnyKeyMissing() {
        // when
        val noSecret = config.clientRegistrationRepository(googleProperties(), KakaoOAuthProperties("rest-key", " "))
        val noId = config.clientRegistrationRepository(googleProperties(), KakaoOAuthProperties("", "secret"))

        // then
        assertNull(noSecret.findByRegistrationId("kakao"))
        assertNull(noId.findByRegistrationId("kakao"))
        assertNotNull(noSecret.findByRegistrationId("google"))
    }

    private fun googleProperties(): OAuth2ClientProperties {
        val properties = OAuth2ClientProperties()
        val google = OAuth2ClientProperties.Registration()
        google.clientId = "google-id"
        google.clientSecret = "google-secret"
        google.scope = setOf("openid", "email", "profile")
        properties.registration["google"] = google
        return properties
    }
}
