package com.sleekydz86.idolglow.user.auth.application.userInfo

class NaverUserInfo(
    attributes: Map<String, Any>,
) : OAuth2UserInfo {
    private val profile: Map<*, *> = attributes["response"] as? Map<*, *> ?: emptyMap<String, Any>()

    override val id: String
        get() =
            (profile["id"] as? String)?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("네이버 회원번호(id)가 없습니다.")

    val usableEmail: String?
        get() = (profile["email"] as? String)?.trim()?.takeIf { it.isNotEmpty() }

    override val email: String
        get() = usableEmail.orEmpty()

    override val name: String?
        get() = profile["nickname"] as? String

    override val picture: String?
        get() = profile["profile_image"] as? String
}
