package com.sleekydz86.idolglow.eventinfo.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("관광 다국어 축제의 한국어 이름 괄호 연결")
class FestivalNameMatchTest {
    @DisplayName("제목 속 한글이 든 마지막 괄호 묶음을 한국어 이름으로 꺼낸다")
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            "ソウルランタンフェスティバル（서울빛초롱축제）|서울빛초롱축제",
            "Gugak Performance Jinyeon (국악공연 진연)|국악공연 진연",
            "APAP作品ツアー（安養公共芸術プロジェクト）（APAP 작품투어（안양공공예술프로젝트））|APAP 작품투어（안양공공예술프로젝트）",
            "广安里M(Marvelous)无人机灯光秀(광안리 M(Marvelous) 드론 라이트쇼)|광안리 M(Marvelous) 드론 라이트쇼",
        ],
    )
    fun extractsLastHangulGroup(
        title: String,
        expected: String,
    ) {
        // when
        val name = KoreanNameInParentheses.extract(title)

        // then
        assertEquals(expected, name)
    }

    @DisplayName("한글이 든 괄호가 없거나 괄호 짝이 맞지 않으면 한국어 이름이 없다")
    @ParameterizedTest(name = "{0}")
    @CsvSource(
        delimiter = '|',
        value = [
            "Busan Christmas Village",
            "APAP作品ツアー（安養公共芸術プロジェクト）",
            "ソウルランタンフェスティバル（서울빛초롱축제",
        ],
    )
    fun noNameWithoutHangulGroup(title: String) {
        // when
        val name = KoreanNameInParentheses.extract(title)

        // then
        assertNull(name)
    }

    @DisplayName("정규화한 한국어 이름이 같고 기간이 하루 이상 겹치는 국문 축제가 1건이면 그 축제를 고른다")
    @Test
    fun picksSingleKoreanFestivalWithSameNameAndOverlappingPeriod() {
        // given
        val japanese = 축제("4135897", "APAP作品ツアー（APAP 작품투어（안양공공예술프로젝트））", "20261101", "20261130")
        val korean =
            listOf(
                축제("3035607", "APAP 작품투어(안양공공예술프로젝트)", "20261130", "20261231"),
                축제("3395188", "서울빛초롱축제", "20261101", "20261130"),
            )

        // when
        val match = FestivalNameMatch.findSingle(japanese, korean)

        // then
        assertEquals("3035607", match?.contentId)
    }

    @DisplayName("이름이 같아도 기간이 하루도 겹치지 않으면 고르지 않는다")
    @Test
    fun skipsWhenPeriodsDoNotOverlap() {
        // given
        val chinese = 축제("1", "首尔灯笼节(서울빛초롱축제)", "20261101", "20261130")
        val korean = listOf(축제("2", "서울빛초롱축제", "20261201", "20261231"))

        // when
        val match = FestivalNameMatch.findSingle(chinese, korean)

        // then
        assertNull(match)
    }

    @DisplayName("이름과 기간이 맞는 국문 축제가 2건이면 애매해서 고르지 않는다")
    @Test
    fun skipsWhenTwoKoreanFestivalsMatch() {
        // given
        val english = 축제("1", "Seoul Lantern Festival (서울빛초롱축제)", "20261101", "20261130")
        val korean = listOf(축제("2", "서울빛초롱축제", "20261101", "20261110"), 축제("3", "서울 빛초롱 축제", "20261120", "20261130"))

        // when
        val match = FestivalNameMatch.findSingle(english, korean)

        // then
        assertNull(match)
    }

    @DisplayName("이름이 다르면 기간이 같아도 고르지 않는다")
    @Test
    fun skipsWhenNameDiffers() {
        // given
        val english = 축제("1", "Seoul Lantern Festival (서울빛초롱축제)", "20261101", "20261130")
        val korean = listOf(축제("2", "부산크리스마스트리문화축제", "20261101", "20261130"))

        // when
        val match = FestivalNameMatch.findSingle(english, korean)

        // then
        assertNull(match)
    }

    private fun 축제(
        contentId: String,
        title: String,
        start: String,
        end: String,
    ): FestivalEvent =
        FestivalEvent(
            contentId = contentId,
            title = title,
            address = null,
            eventStartDate = start,
            eventEndDate = end,
            thumbnailImageUrl = null,
            imageUrl = null,
            mapX = null,
            mapY = null,
            phone = null,
        )
}
