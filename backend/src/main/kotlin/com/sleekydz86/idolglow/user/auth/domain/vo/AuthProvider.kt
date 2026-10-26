package com.sleekydz86.idolglow.user.auth.domain.vo

enum class AuthProvider(
    val registrationId: String,
) {
    GOOGLE("google"),
    KAKAO("kakao"),
    NAVER("naver"),
    TEST("test"),
    ;

    fun accessLabel(): String =
        when (this) {
            GOOGLE -> "구글로그인"
            KAKAO -> "카카오로그인"
            NAVER -> "네이버로그인"
            TEST -> "테스트"
        }

    companion object {
        private val allowedRegistrationIds: Set<String> = setOf(GOOGLE.registrationId, TEST.registrationId)

        fun isAllowedRegistrationId(registrationId: String): Boolean = registrationId in allowedRegistrationIds

        fun fromRegistrationId(registrationId: String): AuthProvider =
            entries.firstOrNull { it.registrationId.equals(registrationId, ignoreCase = true) }
                ?: throw IllegalArgumentException("지원하지 않는 로그인 제공자입니다: $registrationId")
    }
}
