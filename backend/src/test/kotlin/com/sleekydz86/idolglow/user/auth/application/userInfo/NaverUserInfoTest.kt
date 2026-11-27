package com.sleekydz86.idolglow.user.auth.application.userInfo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("네이버 사용자 정보")
class NaverUserInfoTest {
    @DisplayName("response 안의 이메일이 있으면 앞뒤 공백을 뺀 값을 쓸 수 있는 이메일로 준다")
    @Test
    fun usableEmailFromResponse() {
        // given
        val info = NaverUserInfo(attributes(email = "  naver@idolglow.test "))

        // when
        val email = info.usableEmail

        // then
        assertEquals("naver@idolglow.test", email)
        assertEquals("naver@idolglow.test", info.email)
    }

    @DisplayName("이메일이 없거나 비어 있으면 쓸 수 있는 이메일이 없다")
    @ParameterizedTest(name = "{0}")
    @MethodSource("missingEmails")
    fun noUsableEmail(
        label: String,
        email: String?,
    ) {
        // given
        val info = NaverUserInfo(attributes(email = email))

        // when
        val usable = info.usableEmail

        // then
        assertNull(usable, label)
        assertEquals("", info.email, label)
    }

    @DisplayName("회원번호는 response.id 이고, 닉네임과 사진은 response 에서 읽는다")
    @Test
    fun idAndProfile() {
        // given
        val info = NaverUserInfo(attributes())

        // then
        assertEquals("naver-id-1", info.id)
        assertEquals("네이버회원", info.name)
        assertEquals("https://phinf.pstatic.net/profile.png", info.picture)
    }

    @DisplayName("response 가 없거나 회원번호가 없으면 한국어 오류로 거절한다")
    @Test
    fun missingIdIsRejected() {
        // given
        val noResponse = NaverUserInfo(mapOf("resultcode" to "00"))

        // when
        val error = assertThrows(IllegalArgumentException::class.java) { noResponse.id }

        // then
        assertEquals("네이버 회원번호(id)가 없습니다.", error.message)
    }

    companion object {
        private fun attributes(email: String? = "naver@idolglow.test"): Map<String, Any> {
            val response =
                buildMap<String, Any> {
                    put("id", "naver-id-1")
                    email?.let { put("email", it) }
                    put("nickname", "네이버회원")
                    put("profile_image", "https://phinf.pstatic.net/profile.png")
                }
            return mapOf("resultcode" to "00", "message" to "success", "response" to response)
        }

        @JvmStatic
        fun missingEmails(): List<Array<Any?>> =
            listOf(
                arrayOf("이메일 없음", null),
                arrayOf("빈 이메일", ""),
                arrayOf("공백만", "   "),
            )
    }
}
