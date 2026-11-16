package com.sleekydz86.idolglow.eventinfo.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("공연 매칭 장소 비교")
class EventPerformanceMatcherTest {
    @DisplayName("주소형 장소가 공연장명을 포함하면 같은 공연으로 확정한다")
    @Test
    fun addressVenueContainingHallNameIsConfirmed() {
        // given
        val master = master("국립발레단, 호두까기 인형 [김해]", "김해문화의전당")
        val item = performance("[김해] 국립발레단, 호두까기 인형", "경남 김해시 김해문화의전당 마루홀")

        // when
        val decision = EventPerformanceMatcher.decide(item, listOf(master))

        // then
        val expected =
            EventMatchDecision.Attach(
                master.globalId,
                EventMatchStatus.CONFIRMED,
                EventPerformanceMatcher.CONFIRMED_SCORE,
            )
        assertEquals(expected, decision)
    }

    @DisplayName("도시가 다른 같은 제목 공연은 후보로 남긴다")
    @Test
    fun sameTitleInOtherCityStaysCandidate() {
        // given
        val master = master("난타 [제주]", "제주난타극장")
        val item = performance("난타 [명동]", "명동난타극장")

        // when
        val decision = EventPerformanceMatcher.decide(item, listOf(master))

        // then
        assertEquals(CANDIDATE, decision)
    }

    @DisplayName("장소가 포함 관계여도 제목의 지역 표시가 다르면 후보로 남긴다")
    @Test
    fun containedVenueWithDifferentRegionTagStaysCandidate() {
        // given
        val master = master("난타 [제주]", "난타극장")
        val item = performance("난타 [명동]", "명동난타극장")

        // when
        val decision = EventPerformanceMatcher.decide(item, listOf(master))

        // then
        assertEquals(CANDIDATE, decision)
    }

    @DisplayName("지역 표시가 같고 4글자 장소가 포함되면 확정한다")
    @Test
    fun fourLetterVenueWithSameRegionTagIsConfirmed() {
        // given
        val master = master("난타 [명동]", "난타극장")
        val item = performance("난타 [명동]", "명동난타극장")

        // when
        val decision = EventPerformanceMatcher.decide(item, listOf(master))

        // then
        assertEquals(EventMatchStatus.CONFIRMED, (decision as EventMatchDecision.Attach).status)
    }

    @DisplayName("3글자 이하 장소는 포함되어도 같은 장소로 보지 않는다")
    @Test
    fun shortVenueIsNotMatchedByContainment() {
        // given
        val master = master("오늘의 연극", "소극장")
        val item = performance("오늘의 연극", "대학로 예술극장 소극장")

        // when
        val decision = EventPerformanceMatcher.decide(item, listOf(master))

        // then
        assertEquals(CANDIDATE, decision)
    }

    private fun master(
        title: String,
        venue: String,
    ): EventMaster = EventMaster.open(performance(title, venue))

    private fun performance(
        title: String,
        venue: String,
    ): CollectedPerformance =
        CollectedPerformance(
            source = "TEST",
            sourceId = "$title|$venue",
            title = title,
            venue = venue,
            startDate = START,
            endDate = END,
            rawPayload = "$title|$venue",
        )

    companion object {
        private val START: LocalDate = LocalDate.of(2026, 12, 1)
        private val END: LocalDate = LocalDate.of(2026, 12, 20)
        private val CANDIDATE =
            EventMatchDecision.Hold(EventMatchStatus.CANDIDATE, EventPerformanceMatcher.CANDIDATE_SCORE)
    }
}
