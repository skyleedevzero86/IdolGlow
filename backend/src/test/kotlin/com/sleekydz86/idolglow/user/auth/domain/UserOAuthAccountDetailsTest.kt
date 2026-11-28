package com.sleekydz86.idolglow.user.auth.domain

import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import com.sleekydz86.idolglow.user.auth.domain.vo.OAuthAccountDetails
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("SNS 연결의 계정 추가 정보")
class UserOAuthAccountDetailsTest {
    @DisplayName("회원이름은 코드 포인트 100자까지 그대로 두고, 넘으면 100자로 자른다")
    @ParameterizedTest(name = "{0}")
    @MethodSource("names")
    fun accountNameLength(
        label: String,
        input: String,
        expected: String,
    ) {
        // given
        val link = link()

        // when
        link.updateAccountDetails(OAuthAccountDetails(accountName = input, gender = null, ageRange = null))

        // then
        assertEquals(expected, link.accountName, label)
    }

    @DisplayName("이모지를 101자 넣으면 반으로 쪼개지 않고 이모지 100자를 저장한다")
    @Test
    fun emojiIsNotSplit() {
        // given
        val link = link()

        // when
        link.updateAccountDetails(OAuthAccountDetails(accountName = "😀".repeat(101), gender = null, ageRange = null))

        // then
        val stored = link.accountName!!
        assertEquals(100, stored.codePointCount(0, stored.length))
        assertEquals("😀".repeat(100), stored)
    }

    @DisplayName("성별은 10자, 연령대는 20자까지 저장한다")
    @Test
    fun genderAndAgeRangeLength() {
        // given
        val link = link()

        // when
        link.updateAccountDetails(
            OAuthAccountDetails(accountName = null, gender = "M".repeat(11), ageRange = "9".repeat(21)),
        )

        // then
        assertEquals("M".repeat(10), link.gender)
        assertEquals("9".repeat(20), link.ageRange)
    }

    @DisplayName("앞뒤 공백과 U+00A0 을 빼고, 비면 null 로 둔다")
    @Test
    fun blankBecomesNull() {
        // given
        val link = link()

        // when
        link.updateAccountDetails(OAuthAccountDetails(accountName = " 홍길동 ", gender = " ", ageRange = "  "))

        // then
        assertEquals("홍길동", link.accountName)
        assertNull(link.gender)
        assertNull(link.ageRange)
    }

    private fun link(): UserOAuth =
        UserOAuth.of(userId = 1, provider = AuthProvider.NAVER, providerId = "n-1", email = "n@idolglow.test")

    companion object {
        @JvmStatic
        fun names(): List<Array<String>> =
            listOf(
                arrayOf("한글 100자", "가".repeat(100), "가".repeat(100)),
                arrayOf("한글 101자", "가".repeat(101), "가".repeat(100)),
                arrayOf("일본어 101자", "あ".repeat(101), "あ".repeat(100)),
                arrayOf("중국어 101자", "汉".repeat(101), "汉".repeat(100)),
                arrayOf("İ 포함 100자", "İ".repeat(100), "İ".repeat(100)),
            )
    }
}
