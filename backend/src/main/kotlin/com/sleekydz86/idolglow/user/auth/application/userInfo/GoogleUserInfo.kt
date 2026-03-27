package com.sleekydz86.idolglow.user.auth.application.userInfo

class GoogleUserInfo(
    private val attributes: Map<String, Any>
) : OAuth2UserInfo {

    override val id: String
        get() = attributes["sub"] as? String
            ?: throw IllegalArgumentException("구글 사용자 식별자(sub)가 없습니다.")

    override val email: String
        get() = attributes["email"] as? String
            ?: throw IllegalArgumentException("구글 이메일 정보가 없습니다.")

    override val name: String?
        get() = attributes["name"] as? String

    override val picture: String?
        get() = attributes["picture"] as? String
}
