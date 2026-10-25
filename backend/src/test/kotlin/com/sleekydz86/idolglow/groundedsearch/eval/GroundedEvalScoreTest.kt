package com.sleekydz86.idolglow.groundedsearch.eval

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedChecked
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("근거 검색 평가 점수")
class GroundedEvalScoreTest {
    @Test
    @DisplayName("상위 구간 안의 정답 출처만 Recall에 센다")
    fun recallAtFiveCountsExpectedSourcesInsideCutoff() {

        // given
        val ranked = listOf("PRODUCT:1", "PRODUCT:2", "PRODUCT:3")
        val expected = setOf("PRODUCT:1", "PRODUCT:3")

        // when
        val recall = GroundedEvalScore.recallAt(ranked, expected, 2)

        // then
        assertEquals(1.toDouble() / 2.toDouble(), recall)
    }

    @Test
    @DisplayName("첫 정답 순위의 역수를 쓴다")
    fun reciprocalRankUsesFirstExpectedHit() {

        // given
        val ranked = listOf("PRODUCT:1", "PRODUCT:2", "PRODUCT:3")

        // when
        val rank = GroundedEvalScore.reciprocalRank(ranked, setOf("PRODUCT:3"))

        // then
        assertEquals(1.toDouble() / (2 + 1).toDouble(), rank)
    }

    @Test
    @DisplayName("골든셋 예시는 10개이고 한국어와 영어가 섞여 있다")
    fun goldenFileHasTenExamples() {

        // given
        val cases = GroundedGoldenSet.cases()

        // when
        val languages = cases.map { it.language }.toSet()

        // then
        assertEquals((2 shl 2) + 2, cases.size)
        assertTrue("ko" in languages)
        assertTrue("en" in languages)
        assertTrue(cases.all { it.sources.isNotEmpty() })
    }

    @Test
    @DisplayName("가격이나 날짜가 틀린 답은 거절한다")
    fun wrongPriceAndDateAreRejected() {

        // given
        val probes = GroundedGoldenSet.probes()

        // when
        val rejected = GroundedEvalValidation.rejectedCount(probes)

        // then
        assertEquals(probes.size, rejected)
    }

    @Test
    @DisplayName("조건 필드가 같으면 맞다고 센다")
    fun sameConditionFieldsMatch() {

        // given
        val expected = checked("50000")
        val actual = checked("50000.00")

        // when
        val same = GroundedEvalScore.same(actual, expected)

        // then
        assertTrue(same.values.all { it })
    }

    private fun checked(budget: String): GroundedChecked {
        val day = LocalDate.parse("2026-11-01")
        return GroundedChecked(
            region = "마포구",
            dateFrom = day,
            dateTo = day,
            maxBudget = BigDecimal(budget),
            category = "메이크업",
            people = null,
            ignored = emptyList(),
        )
    }
}
