package com.sleekydz86.idolglow.global.config

import com.sleekydz86.idolglow.user.auth.application.KakaoOAuthProperties
import com.sleekydz86.idolglow.user.auth.application.NaverOAuthProperties
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientPropertiesMapper
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod

@Configuration
@EnableConfigurationProperties(OAuth2ClientProperties::class)
class OAuth2ClientRegistrationConfig {
    @Bean
    fun clientRegistrationRepository(
        oAuth2ClientProperties: OAuth2ClientProperties,
        kakaoOAuthProperties: KakaoOAuthProperties,
        naverOAuthProperties: NaverOAuthProperties,
    ): ClientRegistrationRepository {
        val registrations =
            OAuth2ClientPropertiesMapper(oAuth2ClientProperties).asClientRegistrations().values.toMutableList()
        if (kakaoOAuthProperties.configured) {
            registrations += kakaoRegistration(kakaoOAuthProperties)
        }
        if (naverOAuthProperties.configured) {
            registrations += naverRegistration(naverOAuthProperties)
        }
        return InMemoryClientRegistrationRepository(registrations)
    }

    companion object {
        const val KAKAO_REGISTRATION_ID = "kakao"
        private const val KAKAO_AUTHORIZATION_URI = "https://kauth.kakao.com/oauth/authorize"
        private const val KAKAO_TOKEN_URI = "https://kauth.kakao.com/oauth/token"
        private const val KAKAO_USER_INFO_URI = "https://kapi.kakao.com/v2/user/me"
        private const val KAKAO_USER_NAME_ATTRIBUTE = "id"
        private val KAKAO_SCOPES = listOf("profile_nickname", "profile_image", "account_email")
        const val NAVER_REGISTRATION_ID = "naver"
        private const val NAVER_AUTHORIZATION_URI = "https://nid.naver.com/oauth2.0/authorize"
        private const val NAVER_TOKEN_URI = "https://nid.naver.com/oauth2.0/token"
        private const val NAVER_USER_INFO_URI = "https://openapi.naver.com/v1/nid/me"
        private const val NAVER_USER_NAME_ATTRIBUTE = "response"
        private val NAVER_SCOPES = listOf("name", "email", "profile_image")

        fun kakaoRegistration(properties: KakaoOAuthProperties): ClientRegistration =
            ClientRegistration
                .withRegistrationId(KAKAO_REGISTRATION_ID)
                .clientId(properties.clientId)
                .clientSecret(properties.clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope(KAKAO_SCOPES)
                .authorizationUri(KAKAO_AUTHORIZATION_URI)
                .tokenUri(KAKAO_TOKEN_URI)
                .userInfoUri(KAKAO_USER_INFO_URI)
                .userNameAttributeName(KAKAO_USER_NAME_ATTRIBUTE)
                .clientName("Kakao")
                .build()

        fun naverRegistration(properties: NaverOAuthProperties): ClientRegistration =
            ClientRegistration
                .withRegistrationId(NAVER_REGISTRATION_ID)
                .clientId(properties.clientId)
                .clientSecret(properties.clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope(NAVER_SCOPES)
                .authorizationUri(NAVER_AUTHORIZATION_URI)
                .tokenUri(NAVER_TOKEN_URI)
                .userInfoUri(NAVER_USER_INFO_URI)
                .userNameAttributeName(NAVER_USER_NAME_ATTRIBUTE)
                .clientName("Naver")
                .build()
    }
}
