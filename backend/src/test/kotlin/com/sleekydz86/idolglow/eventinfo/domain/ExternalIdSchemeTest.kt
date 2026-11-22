package com.sleekydz86.idolglow.eventinfo.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("외부 ID 체계 매핑과 형식 검사")
class ExternalIdSchemeTest {
    @DisplayName("공급처 이름 6개가 체계 4개로 나뉜다")
    @ParameterizedTest
    @CsvSource(
        "KOPIS_PBLPRFR, KOPIS",
        "KOPIS_PRFFEST, KOPIS",
        "TOUR_API, KTO",
        "SEOUL_SJW_PERFORM, SJW",
        "CULTURE_INFO, CULTURE",
        "CULTURE_CALENDAR, CULTURE",
    )
    fun mapsSourceToScheme(
        source: String,
        expected: ExternalIdScheme,
    ) {
        // when
        val scheme = ExternalIdScheme.ofSource(source)

        // then
        assertEquals(expected, scheme)
    }

    @DisplayName("위키데이터 번호는 Q 뒤에 0이 아닌 숫자로 시작해야 한다")
    @ParameterizedTest
    @CsvSource("Q1, true", "Q12345, true", "Q0, false", "Q012, false", "12345, false", "q12, false", "Q, false")
    fun checksWikidataFormat(
        value: String,
        expected: Boolean,
    ) {
        // when
        val valid = ExternalIdScheme.WIKIDATA.validFormat(value)

        // then
        assertEquals(expected, valid)
    }

    @DisplayName("체계 이름은 대소문자를 가리지 않고, 모르는 이름은 거절한다")
    @Test
    fun parsesSchemeName() {
        // when
        val parsed = ExternalIdScheme.parse(" wikidata ")
        val unknown = assertThrows<IllegalArgumentException> { ExternalIdScheme.parse("TMDB") }

        // then
        assertEquals(ExternalIdScheme.WIKIDATA, parsed)
        assertEquals(null, ExternalIdScheme.ofSource("UNKNOWN_SOURCE"))
        assertEquals("알 수 없는 외부 ID 체계입니다. scheme=TMDB", unknown.message)
    }
}
