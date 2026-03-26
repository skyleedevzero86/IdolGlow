package com.sleekydz86.idolglow.user.auth.application.userInfo

class GoogleUserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {

    override val id: String
        get() = attributes["sub"] as? String
            ?: throw IllegalArgumentException("Google 'sub' is missing")

    override val email: String
        get() = attributes["email"] as? String
            ?: throw IllegalArgumentException("Google 'email' is missing")

    override val name: String?
        get() = attributes["name"] as? String
}