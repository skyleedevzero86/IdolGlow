package com.sleekydz86.idolglow.luckydraw.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("당첨자 닉네임 가리기")
class NicknameMaskTest {
    @DisplayName("D27 4자보다 길면 앞 4자, 4자 이하면 앞 1자만 보인다")
    @ParameterizedTest
    @CsvSource(
        "idolglow, idol***",
        "glowy, glow***",
        "glow, g***",
        "민지, 민***",
        "아이돌글로우, 아이돌글***",
        "🎤🎤🎤🎤🎤, 🎤🎤🎤🎤***",
    )
    fun mask(
        nickname: String,
        expected: String,
    ) {
        // when
        val masked = NicknameMask.mask(nickname)

        // then
        assertEquals(expected, masked)
    }
}
