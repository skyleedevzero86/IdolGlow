package com.sleekydz86.idolglow.user.auth.oauth

import com.sleekydz86.idolglow.user.auth.application.userInfo.OAuth2UserInfo
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService : OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private val delegate = DefaultOAuth2UserService()

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val delegateUser = delegate.loadUser(userRequest)
        val provider = AuthProvider.fromRegistrationId(userRequest.clientRegistration.registrationId)
        val userInfo = OAuth2UserInfo.of(provider, delegateUser.attributes)
        return CustomOAuth2User(provider, userInfo, delegateUser)
    }
}
