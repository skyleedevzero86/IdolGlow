package com.sleekydz86.idolglow.user.auth.application

import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.stereotype.Service

@Service
class OAuthProviderAvailability(
    private val clientRegistrationRepository: ClientRegistrationRepository,
) {
    fun assertLoginAvailable(registrationId: String) {
        require(AuthProvider.isAllowedRegistrationId(registrationId)) {
            "지원하지 않는 로그인 제공자입니다: $registrationId"
        }
        require(clientRegistrationRepository.findByRegistrationId(registrationId) != null) {
            "로그인 설정이 없는 제공자입니다: $registrationId"
        }
    }
}
