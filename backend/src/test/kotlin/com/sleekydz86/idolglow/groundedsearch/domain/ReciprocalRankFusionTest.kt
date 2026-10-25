package com.sleekydz86.idolglow.groundedsearch.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("근거 검색 순위 합치기")
class ReciprocalRankFusionTest {
    @Test
    @DisplayName("키워드 순위와 벡터 순위를 합쳐 둘 다 나온 문단을 앞에 둔다")
    fun rrfMergesKeywordAndVectorRanks() {
        // given
        val firstId = 1L
        val secondId = 2L
        val thirdId = firstId + secondId
        val keywordIds = listOf(firstId, secondId)
        val vectorIds = listOf(secondId, thirdId)

        // when
        val fused =
            ReciprocalRankFusion.merge(
                keywordIds,
                vectorIds,
                GroundedSearchLimits.rrfK(),
                GroundedSearchLimits.resultLimit(),
            )

        // then
        assertEquals(listOf(secondId, firstId, thirdId), fused.map { it.chunkId })
        assertEquals(2, fused.first().keywordRank)
        assertEquals(1, fused.first().vectorRank)
        assertEquals(1, fused[1].keywordRank)
        assertNull(fused[1].vectorRank)
    }
}
