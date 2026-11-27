package com.sleekydz86.idolglow.user.auth.application.userInfo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("카카오 사용자 정보")
class KakaoUserInfoTest {
    @DisplayName("K3 이메일이 있고 유효·인증이면 쓸 수 있는 이메일로 앞뒤 공백을 뺀 값을 준다")
    @Test
    fun usableEmailWhenValidAndVerified() {
        // given
        val info = KakaoUserInfo(attributes(account(email = "  kakao@idolglow.test  ")))

        // when
        val email = info.usableEmail

        // then
        assertEquals("kakao@idolglow.test", email)
        assertEquals("kakao@idolglow.test", info.email)
    }

    @DisplayName("K3 이메일 조건 하나라도 어기면 쓸 수 있는 이메일이 없다")
    @ParameterizedTest(name = "{0}")
    @MethodSource("unusableAccounts")
    fun noUsableEmail(
        label: String,
        account: Map<String, Any?>,
    ) {
        // given
        val info = KakaoUserInfo(attributes(account))

        // when
        val email = info.usableEmail

        // then
        assertNull(email, label)
        assertEquals("", info.email, label)
    }

    @DisplayName("K1 회원번호는 최상위 id 를 문자열로 쓴다")
    @Test
    fun idFromTopLevel() {
        // given
        val info = KakaoUserInfo(attributes(account()))

        // when
        val id = info.id

        // then
        assertEquals("4012345678", id)
    }

    @DisplayName("회원번호가 없으면 한국어 오류로 거절한다")
    @Test
    fun missingIdIsRejected() {
        // given
        val info = KakaoUserInfo(mapOf("kakao_account" to account()))

        // when
        val error = assertThrows(IllegalArgumentException::class.java) { info.id }

        // then
        assertEquals("카카오 회원번호(id)가 없습니다.", error.message)
    }

    @DisplayName("K8 닉네임과 사진은 kakao_account.profile 에서 읽고, 없으면 null 이다")
    @Test
    fun profileFields() {
        // given
        val withProfile = KakaoUserInfo(attributes(account()))
        val withoutProfile = KakaoUserInfo(attributes(account().minus("profile")))

        // then
        assertEquals("응모회원", withProfile.name)
        assertEquals("https://k.kakaocdn.net/profile.jpg", withProfile.picture)
        assertNull(withoutProfile.name)
        assertNull(withoutProfile.picture)
    }

    companion object {
        private fun attributes(account: Map<String, Any?>): Map<String, Any> =
            mapOf("id" to 4012345678L, "kakao_account" to account.filterValues { it != null })

        private fun account(
            email: String? = "kakao@idolglow.test",
            valid: Any? = true,
            verified: Any? = true,
        ): Map<String, Any?> =
            mapOf(
                "email" to email,
                "is_email_valid" to valid,
                "is_email_verified" to verified,
                "profile" to mapOf("nickname" to "응모회원", "profile_image_url" to "https://k.kakaocdn.net/profile.jpg"),
            )

        @JvmStatic
        fun unusableAccounts(): List<Array<Any>> =
            listOf(
                arrayOf("이메일 없음", account(email = null)),
                arrayOf("빈 이메일", account(email = "   ")),
                arrayOf("유효하지 않음", account(valid = false)),
                arrayOf("인증 안 됨", account(verified = false)),
                arrayOf("유효 여부 없음", account(valid = null)),
                arrayOf("인증 여부 없음", account(verified = null)),
                arrayOf("문자열 true 는 인정 안 함", account(verified = "true")),
            )
    }
}
