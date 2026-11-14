package com.sleekydz86.idolglow.survey.application

import com.sleekydz86.idolglow.productpackage.discovery.application.RecommendationSignals
import com.sleekydz86.idolglow.survey.domain.RecommendationSignalType
import com.sleekydz86.idolglow.user.user.domain.vo.ConceptType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("설문지 폼 응답 → 추천 신호")
class SurveySignalExtractorTest {
    @Test
    @DisplayName("CONCEPT 문항의 선택지 '걸크러시'는 GIRL_CRUSH로, 못 바꾸는 선택지는 버린다")
    fun extractorMapsConceptOptionToEnum() {
        // given
        val submission =
            SurveyFormFixtures()
                .choice(RecommendationSignalType.CONCEPT, "걸크러시", "러블리/청순", "레트로")
                .build()

        // when
        val signals = SurveySignalExtractor.extract(submission)

        // then
        assertEquals(setOf(ConceptType.GIRL_CRUSH.name, ConceptType.LOVELY_FRESH.name), signals.conceptTags)
    }

    @Test
    @DisplayName("신호 종류가 없는 문항은 무시하고 날짜는 넣지 않는다")
    fun extractorIgnoresQuestionsWithoutSignal() {
        // given
        val submission =
            SurveyFormFixtures()
                .choice(null, "걸크러시")
                .text(null, "성동구 BTS")
                .build()

        // when
        val signals = SurveySignalExtractor.extract(submission)

        // then
        assertTrue(signals.isEmpty())
        assertNull(signals.visitStart)
        assertNull(signals.visitEnd)
    }

    @Test
    @DisplayName("IDOL은 2글자 이상 토큰, PLACE는 글자 그대로, TAG는 선택지 그대로 쓴다")
    fun extractorMapsIdolPlaceAndTag() {
        // given
        val submission =
            SurveyFormFixtures()
                .text(RecommendationSignalType.IDOL, "BTS, 뉴진스 a")
                .text(RecommendationSignalType.PLACE, "성수, 홍대")
                .choice(RecommendationSignalType.PLACE, "성동구")
                .choice(RecommendationSignalType.TAG, "포토 스튜디오")
                .build()

        // when
        val signals = SurveySignalExtractor.extract(submission)

        // then
        assertEquals(
            RecommendationSignals(
                idolTokens = listOf("BTS", "뉴진스"),
                placeKeywords = listOf("성수", "홍대", "성동구"),
                extraTags = setOf("포토 스튜디오"),
            ),
            signals,
        )
    }
}
