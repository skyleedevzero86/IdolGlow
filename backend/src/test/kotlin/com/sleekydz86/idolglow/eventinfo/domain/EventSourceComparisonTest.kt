package com.sleekydz86.idolglow.eventinfo.domain

import com.sleekydz86.idolglow.eventinfo.application.EventMatchProposalScore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class EventSourceComparisonTest {
    @Test
    fun subtitleWithSameVenueAndPeriodIsSameShow() {
        val facts = facts("신해경 [서울]", "예술의전당", "신해경", "예술의전당", SAME_START, SAME_END, SAME_START, SAME_END)

        assertEquals(true, facts.sameShow())
        assertEquals(true, facts.localReason().contains("부제만 다릅니다"))
    }

    @Test
    fun differentVenueIsNotSameShow() {
        val facts = facts("호두까기 인형", "예술의전당", "호두까기 인형", "국립극장", SAME_START, SAME_END, SAME_START, SAME_END)

        assertEquals(false, facts.sameShow())
        assertEquals(true, facts.periodsOverlap)
        assertEquals(true, facts.localReason().contains("장소는 다릅니다"))
    }

    @Test
    fun differentPeriodIsNotSameShow() {
        val nextYear = SAME_START.plusYears(1)
        val facts = facts("백자한상", "국립중앙박물관", "백자한상", "국립중앙박물관", SAME_START, SAME_END, nextYear, nextYear)

        assertEquals(false, facts.periodsOverlap)
        assertEquals(false, facts.sameShow())
    }

    @Test
    fun summaryCountsWrongProposalsThatStayUnapplied() {
        val score = EventMatchProposalScore(total = 2, correct = 1, wrong = 1)

        assertEquals("AI 제안 2건 중 1건 맞음, 틀린 1건은 승인 단계에서 걸러짐", score.summary())
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
