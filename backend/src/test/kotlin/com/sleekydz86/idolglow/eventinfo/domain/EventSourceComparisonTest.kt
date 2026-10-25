package com.sleekydz86.idolglow.eventinfo.domain

import com.sleekydz86.idolglow.eventinfo.application.EventMatchProposalScore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("공연 원본 비교")
class EventSourceComparisonTest {
    @DisplayName("부제만 다르고 장소와 기간이 같으면 같은 공연이다")
    @Test
    fun subtitleWithSameVenueAndPeriodIsSameShow() {

        // given
        val facts =
            facts(
                "신해경 [서울]",
                "예술의전당",
                "신해경",
                "예술의전당",
                SAME_START,
                SAME_END,
                SAME_START,
                SAME_END,
            )

        // when
        val sameShow = facts.sameShow()
        val reason = facts.localReason()

        // then
        assertEquals(true, sameShow)
        assertEquals(true, reason.contains("부제만 다릅니다"))
    }

    @DisplayName("장소가 다르면 같은 공연이 아니다")
    @Test
    fun differentVenueIsNotSameShow() {

        // given
        val facts =
            facts(
                "호두까기 인형",
                "예술의전당",
                "호두까기 인형",
                "국립극장",
                SAME_START,
                SAME_END,
                SAME_START,
                SAME_END,
            )

        // when
        val sameShow = facts.sameShow()

        // then
        assertEquals(false, sameShow)
        assertEquals(true, facts.periodsOverlap)
        assertEquals(true, facts.localReason().contains("장소는 다릅니다"))
    }

    @DisplayName("기간이 겹치지 않으면 같은 공연이 아니다")
    @Test
    fun differentPeriodIsNotSameShow() {

        // given
        val nextYear = SAME_START.plusYears(1)
        val facts =
            facts(
                "백자한상",
                "국립중앙박물관",
                "백자한상",
                "국립중앙박물관",
                SAME_START,
                SAME_END,
                nextYear,
                nextYear,
            )

        // when
        val overlap = facts.periodsOverlap
        val sameShow = facts.sameShow()

        // then
        assertEquals(false, overlap)
        assertEquals(false, sameShow)
    }

    @DisplayName("틀린 제안은 맞음 수에서 빼고 승인 전 문구로 센다")
    @Test
    fun summaryCountsWrongProposalsThatStayUnapplied() {

        // given
        val score = EventMatchProposalScore(total = 2, correct = 1, wrong = 1)

        // when
        val summary = score.summary()

        // then
        assertEquals("AI 제안 2건 중 1건 맞음, 틀린 1건은 승인 단계에서 걸러짐", summary)
    }

    private fun facts(
        leftTitle: String,
        leftVenue: String,
        rightTitle: String,
        rightVenue: String,
        leftStart: LocalDate,
        leftEnd: LocalDate,
        rightStart: LocalDate,
        rightEnd: LocalDate,
    ): EventMatchFacts =
        EventSourceComparison.of(
            EventMatchSide(leftTitle, leftStart, leftEnd, leftVenue, "left"),
            EventMatchSide(rightTitle, rightStart, rightEnd, rightVenue, "right"),
        )

    companion object {
        private val SAME_START: LocalDate = LocalDate.of(2026, 11, 1)
        private val SAME_END: LocalDate = LocalDate.of(2026, 11, 3)
    }
}
