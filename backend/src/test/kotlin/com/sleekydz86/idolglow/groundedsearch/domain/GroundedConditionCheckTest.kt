package com.sleekydz86.idolglow.groundedsearch.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("근거 검색 조건 검증")
class GroundedConditionCheckTest {
    @Test
    @DisplayName("틀린 추출 조건은 무시한다")
    fun invalidExtractedConditionIsIgnored() {

        // given
        val draft =
            GroundedConditionDraft(
                region = "부산",
                dateFrom = "어제",
                maxBudget = "-1",
                people = "여럿",
            )

        // when
        val checked = GroundedConditionCheck.accept(draft)

        // then
        assertNull(checked.region)
        assertNull(checked.dateFrom)
        assertNull(checked.dateTo)
        assertNull(checked.maxBudget)
        assertNull(checked.people)
        assertEquals(listOf("지역", "날짜", "예산", "인원"), checked.ignored)
        assertTrue(checked.ignored.isNotEmpty())
    }
}
