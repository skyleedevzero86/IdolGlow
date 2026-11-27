package com.sleekydz86.idolglow.user.auth.application

import com.sleekydz86.idolglow.global.config.OAuth2ClientRegistrationConfig
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository

@DisplayName("로그인 제공자 사용 가능 여부")
class OAuthProviderAvailabilityTest {
    private val kakao = OAuth2ClientRegistrationConfig.kakaoRegistration(KakaoOAuthProperties("rest-key", "secret"))

    @DisplayName("R16 네이버는 허용 목록 밖이라 등록이 있어도 거절한다")
    @Test
    fun naverIsNotAllowed() {
        // given
        val availability = OAuthProviderAvailability(InMemoryClientRegistrationRepository(kakao))

        // when
        val error = assertThrows(IllegalArgumentException::class.java) { availability.assertLoginAvailable("naver") }

        // then
        assertEquals("지원하지 않는 로그인 제공자입니다: naver", error.message)
    }

    @DisplayName("결정 2 카카오는 허용되지만 등록이 없으면 설정 없음으로 거절한다")
    @Test
    fun kakaoWithoutRegistrationIsRejected() {
        // given
        val google =
            OAuth2ClientRegistrationConfig
                .kakaoRegistration(KakaoOAuthProperties("google-id", "google-secret"))
                .let { org.springframework.security.oauth2.client.registration.ClientRegistration.withClientRegistration(it).registrationId("google").build() }
        val availability = OAuthProviderAvailability(InMemoryClientRegistrationRepository(google))

        // when
        val error = assertThrows(IllegalArgumentException::class.java) { availability.assertLoginAvailable("kakao") }

        // then
        assertEquals("로그인 설정이 없는 제공자입니다: kakao", error.message)
    }

    @DisplayName("R1 카카오 등록이 있으면 통과한다")
    @Test
    fun kakaoWithRegistrationPasses() {
        // given
        val availability = OAuthProviderAvailability(InMemoryClientRegistrationRepository(kakao))

        // then
        assertDoesNotThrow { availability.assertLoginAvailable("kakao") }
    }
}
