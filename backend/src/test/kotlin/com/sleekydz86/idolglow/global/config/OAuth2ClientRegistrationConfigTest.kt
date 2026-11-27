package com.sleekydz86.idolglow.global.config

import com.sleekydz86.idolglow.user.auth.application.KakaoOAuthProperties
import com.sleekydz86.idolglow.user.auth.application.NaverOAuthProperties
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
        val repository =
            repository(KakaoOAuthProperties("rest-key", "secret"), NaverOAuthProperties())

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
        val noSecret = repository(KakaoOAuthProperties("rest-key", " "), NaverOAuthProperties())
        val noId = repository(KakaoOAuthProperties("", "secret"), NaverOAuthProperties())

        // then
        assertNull(noSecret.findByRegistrationId("kakao"))
        assertNull(noId.findByRegistrationId("kakao"))
        assertNotNull(noSecret.findByRegistrationId("google"))
    }

    @DisplayName("네이버 키가 둘 다 있으면 네이버 등록을 더하고, 하나라도 비면 더하지 않는다")
    @Test
    fun addsNaverOnlyWhenConfigured() {
        // when
        val withNaver =
            repository(KakaoOAuthProperties(), NaverOAuthProperties("naver-id", "naver-secret"))
        val withoutSecret =
            repository(KakaoOAuthProperties(), NaverOAuthProperties("naver-id", ""))

        // then
        val naver = withNaver.findByRegistrationId("naver")
        assertNotNull(naver)
        assertEquals("https://nid.naver.com/oauth2.0/authorize", naver!!.providerDetails.authorizationUri)
        assertEquals("https://nid.naver.com/oauth2.0/token", naver.providerDetails.tokenUri)
        assertEquals("https://openapi.naver.com/v1/nid/me", naver.providerDetails.userInfoEndpoint.uri)
        assertEquals("response", naver.providerDetails.userInfoEndpoint.userNameAttributeName)
        assertEquals(ClientAuthenticationMethod.CLIENT_SECRET_POST, naver.clientAuthenticationMethod)
        assertNull(withNaver.findByRegistrationId("kakao"))
        assertNull(withoutSecret.findByRegistrationId("naver"))
    }

    private fun repository(
        kakao: KakaoOAuthProperties,
        naver: NaverOAuthProperties,
    ) = config.clientRegistrationRepository(googleProperties(), kakao, naver)

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
