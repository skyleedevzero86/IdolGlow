package com.sleekydz86.idolglow.global.infrastructure.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.UUID

@DisplayName("로그 민감정보 가리기")
class SensitiveLogMaskerTest {
    @DisplayName("이름으로 알 수 있는 민감정보는 값만 가린다")
    @ParameterizedTest(name = "{0}")
    @CsvSource(
        delimiter = '|',
        value = [
            "Authorization 헤더|Authorization: Bearer abc.DEF-123_xyz|Authorization: Bearer ****",
            "Basic 인증|Authorization: Basic dXNlcjpwYXNz|Authorization: Basic ****",
            "쿼리 serviceKey|GET /api?serviceKey=AbCd1234%2Bxy&page=1|GET /api?serviceKey=****&page=1",
            "쿼리 authkey|https://x.test/rest?authkey=k3y-v4lue|https://x.test/rest?authkey=****",
            "토큰 키값|refreshToken=r1.r2.r3 기록|refreshToken=**** 기록",
            "쿠키|Cookie: JSESSIONID=ABC123; theme=dark|Cookie: JSESSIONID=****; theme=dark",
            "JSON 비밀번호|{\"email\":\"a@b.c\",\"password\":\"p@ss w0rd\"}|{\"email\":\"a@b.c\",\"password\":\"****\"}",
            "JSON 시크릿|{\"secretKey\" : \"live_sk_123\"}|{\"secretKey\" : \"****\"}",
            "텔레그램 봇 주소|POST https://api.telegram.org/bot123456:AA-bb_cc/sendMessage|" +
                "POST https://api.telegram.org/bot****/sendMessage",
            "디스코드 웹훅|https://discord.com/api/webhooks/111/abc-DEF 실패|https://discord.com/api/webhooks/**** 실패",
        ],
    )
    fun masksNamedSecrets(
        case: String,
        input: String,
        expected: String,
    ) {
        // given
        val text = input

        // when
        val masked = SensitiveLogMasker.mask(text)

        // then
        assertEquals(expected, masked, case)
    }

    @DisplayName("JWT 모양의 문자열은 이름이 없어도 가린다")
    @Test
    fun masksJwtWithoutName() {
        // given
        val jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.c2lnbmF0dXJlX3ZhbHVl"

        // when
        val masked = SensitiveLogMasker.mask("응답 본문 $jwt 끝")

        // then
        assertEquals("응답 본문 **** 끝", masked)
    }

    @DisplayName("등록된 설정 값은 주소 경로 안에 들어 있어도 가린다")
    @Test
    fun masksRegisteredValueInsidePath() {
        // given
        val key = "pathKey-" + UUID.randomUUID()
        SensitiveLogMasker.registerSecretValues(listOf(key))

        // when
        val masked = SensitiveLogMasker.mask("연결 실패 http://openapi.test/$key/xml/list/1/5")

        // then
        assertEquals("연결 실패 http://openapi.test/****/xml/list/1/5", masked)
    }

    @DisplayName("민감정보가 아닌 로그는 그대로 둔다")
    @Test
    fun keepsOrdinaryLog() {
        // given
        val text = "결제 승인 완료 orderId=pay_toss_123 idempotencyKey=abc amount=15000 cacheKey=home"

        // when
        val masked = SensitiveLogMasker.mask(text)

        // then
        assertEquals(text, masked)
    }

    @DisplayName("빈 값과 null 은 그대로 돌려준다")
    @Test
    fun keepsNullAndEmpty() {
        // given
        val empty = ""

        // when
        val maskedEmpty = SensitiveLogMasker.mask(empty)
        val maskedNull = SensitiveLogMasker.mask(null)

        // then
        assertEquals("", maskedEmpty)
        assertNull(maskedNull)
    }

    @DisplayName("긴 값을 먼저 가려 짧은 값의 일부만 가려진 채 남지 않는다")
    @Test
    fun masksLongerValueFirst() {
        // given
        val short = "shortKey" + UUID.randomUUID().toString().take(4)
        val long = short + "-extended"
        SensitiveLogMasker.registerSecretValues(listOf(short, long))

        // when
        val masked = SensitiveLogMasker.mask("값 $long 끝")!!

        // then
        assertEquals("값 **** 끝", masked)
        assertFalse(masked.contains("extended"))
        assertTrue(masked.contains(SensitiveLogMasker.MASK))
    }
}
