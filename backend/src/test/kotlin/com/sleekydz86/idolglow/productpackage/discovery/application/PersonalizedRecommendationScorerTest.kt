package com.sleekydz86.idolglow.productpackage.discovery.application

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.user.user.domain.UserSurvey
import com.sleekydz86.idolglow.user.user.domain.vo.ConceptType
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("개인화 추천 점수")
class PersonalizedRecommendationScorerTest {
    private val product = Product(id = 1L, name = "메이크업 체험", description = "아이돌 무대 메이크업")
    private val noMetrics =
        RecommendationProductMetrics(availableTripDays = 0, wishCount = 0, averageRating = 0.0, reviewCount = 0)

    private fun survey(
        concept: ConceptType,
        start: LocalDate = LocalDate.of(2026, 12, 1),
        end: LocalDate = LocalDate.of(2026, 12, 4),
    ): UserSurvey {
        val survey = mockk<UserSurvey>()
        every { survey.concept } returns concept
        every { survey.idolName } returns "x"
        every { survey.places } returns mutableListOf()
        every { survey.visitStartDate } returns start
        every { survey.visitEndDate } returns end
        return survey
    }

    private fun score(
        signals: RecommendationSignals,
        tags: Set<String>,
        metrics: RecommendationProductMetrics = noMetrics,
    ) = PersonalizedRecommendationScorer.score(signals, tags, product, metrics)

    @Test
    @DisplayName("태그가 '걸크러시'인 상품은 GIRL_CRUSH 설문에서 컨셉 이유를 받는다")
    fun conceptMatchesKoreanTag() {
        // given
        val signals = RecommendationSignals.fromUserSurvey(survey(ConceptType.GIRL_CRUSH), emptySet())

        // when
        val result = score(signals, setOf("걸크러시"))

        // then
        val reason = result.reasons.single { it.code == RecommendationReasonCode.CONCEPT_MATCH }
        assertEquals("선호 컨셉 '걸크러시' 태그", reason.label)
        assertEquals(4.0, result.score, 1e-9)
    }

    @Test
    @DisplayName("태그가 'GIRL_CRUSH'여도 같은 결과다")
    fun conceptMatchesEnumNameTag() {
        // given
        val signals = RecommendationSignals.fromUserSurvey(survey(ConceptType.GIRL_CRUSH), emptySet())

        // when
        val korean = score(signals, setOf("걸크러시"))
        val enumName = score(signals, setOf("GIRL_CRUSH"))

        // then
        assertEquals(korean.score, enumName.score, 1e-9)
        assertEquals(korean.reasons, enumName.reasons)
    }

    @Test
    @DisplayName("러블리/청순처럼 나눠 쓴 태그도 조각 단위로 맞춘다")
    fun conceptMatchesSplitTag() {
        // given
        val signals = RecommendationSignals.fromUserSurvey(survey(ConceptType.LOVELY_FRESH), emptySet())

        // when
        val result = score(signals, setOf("러블리 / 청순"))

        // then
        assertTrue(result.reasons.any { it.code == RecommendationReasonCode.CONCEPT_MATCH })
    }

    @Test
    @DisplayName("ETC 컨셉은 컨셉 점수를 더하지 않는다")
    fun etcConceptAddsNoConceptScore() {
        // given
        val signals = RecommendationSignals.fromUserSurvey(survey(ConceptType.ETC), emptySet())

        // when
        val result = score(signals, setOf("ETC", "기타"))

        // then
        assertTrue(signals.conceptTags.isEmpty())
        assertTrue(result.reasons.none { it.code == RecommendationReasonCode.CONCEPT_MATCH })
        assertEquals(0.0, result.score, 1e-9)
    }

    @Test
    @DisplayName("방문 4일 중 3일 예약 가능하면 일정 점수 3.75와 이유를 준다")
    fun scheduleFitRatioUsesAvailableDays() {
        // given
        val signals =
            RecommendationSignals(visitStart = LocalDate.of(2026, 12, 1), visitEnd = LocalDate.of(2026, 12, 4))
        val metrics = noMetrics.copy(availableTripDays = 3)

        // when
        val result = score(signals, emptySet(), metrics)

        // then
        assertEquals(3.75, result.score, 1e-9)
        val reason = result.reasons.single { it.code == RecommendationReasonCode.SCHEDULE_FIT }
        assertTrue(reason.label.contains("4일") && reason.label.contains("3일"))
    }

    @Test
    @DisplayName("폼 TAG 신호는 행동 태그와 같은 2.5점이고 찜·평점·리뷰 점수는 이유에 없다")
    fun extraTagUsesBehaviorWeight() {
        // given
        val signals = RecommendationSignals(extraTags = setOf("메이크업"))
        val metrics = noMetrics.copy(wishCount = 10, averageRating = 4.5, reviewCount = 3)

        // when
        val withTag = score(signals, setOf("메이크업"), metrics)
        val withoutTag = score(signals, setOf("헤어"), metrics)

        // then
        assertEquals(2.5, withTag.score - withoutTag.score, 1e-9)
        assertEquals(listOf(RecommendationReasonCode.EXTRA_TAG), withTag.reasons.map { it.code })
        assertTrue(withoutTag.reasons.isEmpty())
    }

    @Test
    @DisplayName("폼 신호와 합쳐도 방문 기간은 회원 설문 값을 쓴다")
    fun mergeKeepsUserSurveyDates() {
        // given
        val user = RecommendationSignals.fromUserSurvey(survey(ConceptType.DREAMY), setOf("메이크업"))
        val form =
            RecommendationSignals(
                conceptTags = setOf(ConceptType.GIRL_CRUSH.name),
                placeKeywords = listOf("성수"),
                visitStart = LocalDate.of(2027, 1, 1),
                visitEnd = LocalDate.of(2027, 1, 2),
            )

        // when
        val merged = user.merge(form)

        // then
        assertEquals(LocalDate.of(2026, 12, 1), merged.visitStart)
        assertEquals(LocalDate.of(2026, 12, 4), merged.visitEnd)
        assertEquals(setOf("DREAMY", "GIRL_CRUSH"), merged.conceptTags)
        assertEquals(listOf("성수"), merged.placeKeywords)
        assertEquals(setOf("메이크업"), merged.behaviorTags)
    }
}
