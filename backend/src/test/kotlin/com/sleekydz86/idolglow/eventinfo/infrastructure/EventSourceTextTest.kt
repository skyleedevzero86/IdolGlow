package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.EventTitleNormalizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("공급처 글자의 HTML 기호 풀기")
class EventSourceTextTest {
    @DisplayName("이름 기호와 숫자 기호를 원래 글자로 바꾼다")
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            "연극 &lt;먼지여자&gt;|연극 <먼지여자>",
            "Tom &amp; Jerry|Tom & Jerry",
            "&ldquo;봄&rdquo; &middot; 여름|“봄” · 여름",
            "작가&#39;s 노트|작가's 노트",
            "대학로&nbsp;&nbsp;극장|대학로 극장",
            "그대로인 제목|그대로인 제목",
        ],
    )
    fun decodesEntities(
        raw: String,
        expected: String,
    ) {
        // when
        val decoded = EventSourceText.decode(raw)

        // then
        assertEquals(expected, decoded)
    }

    @DisplayName("기호를 풀면 다른 공급처의 같은 제목과 매칭용 제목이 같아진다")
    @Test
    fun decodedTitleMatchesPlainSource() {
        // given
        val escaped = "연극 &lt;먼지여자&gt;"
        val plain = "연극 <먼지여자>"

        // when
        val normalized = EventTitleNormalizer.normalize(EventSourceText.decode(escaped))

        // then
        assertEquals(EventTitleNormalizer.normalize(plain), normalized)
        assertEquals("연극먼지여자", normalized)
    }

    @DisplayName("풀고 나서 비면 null로 돌려준다")
    @Test
    fun blankBecomesNull() {
        // when
        val decoded = EventSourceText.decodeOrNull("&nbsp;")

        // then
        assertNull(decoded)
    }
}
