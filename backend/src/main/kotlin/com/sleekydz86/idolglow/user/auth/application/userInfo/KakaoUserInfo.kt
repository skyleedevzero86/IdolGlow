package com.sleekydz86.idolglow.user.auth.application.userInfo

class KakaoUserInfo(
    private val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    private val account: Map<*, *> = attributes["kakao_account"] as? Map<*, *> ?: emptyMap<String, Any>()

    private val profile: Map<*, *> = account["profile"] as? Map<*, *> ?: emptyMap<String, Any>()

    override val id: String
        get() =
            attributes["id"]?.toString()?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("카카오 회원번호(id)가 없습니다.")

    val usableEmail: String?
        get() {
            val email = (account["email"] as? String)?.trim()
            val valid = account["is_email_valid"] == true
            val verified = account["is_email_verified"] == true
            return email?.takeIf { it.isNotEmpty() && valid && verified }
        }

    override val email: String
        get() = usableEmail.orEmpty()

    override val name: String?
        get() = profile["nickname"] as? String

    override val picture: String?
        get() = profile["profile_image_url"] as? String
}
