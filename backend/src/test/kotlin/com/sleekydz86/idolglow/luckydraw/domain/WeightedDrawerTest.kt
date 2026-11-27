package com.sleekydz86.idolglow.luckydraw.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("가중 무작위 비복원 추첨")
class WeightedDrawerTest {
    private val candidates =
        listOf(DrawCandidate(30L, 12L), DrawCandidate(10L, 1L), DrawCandidate(20L, 3L), DrawCandidate(40L, 1L))

    @DisplayName("D22 같은 씨앗과 같은 후보면 후보 순서가 달라도 같은 당첨자 순서가 나온다")
    @Test
    fun reproducibleWithSeed() {
        // when
        val first = WeightedDrawer.draw(20261208L, candidates, 3)
        val second = WeightedDrawer.draw(20261208L, candidates.reversed(), 3)

        // then
        assertEquals(first, second)
    }

    @DisplayName("D20 한 회원은 한 번만 당첨되고 당첨자 수만큼 뽑는다")
    @Test
    fun withoutReplacement() {
        // when
        val winners = WeightedDrawer.draw(1L, candidates, 3)

        // then
        assertEquals(3, winners.size)
        assertEquals(3, winners.toSet().size)
    }

    @DisplayName("D21 후보가 당첨자 수보다 적으면 전원, 후보가 없으면 빈 목록")
    @Test
    fun fewerCandidates() {
        // when
        val all = WeightedDrawer.draw(5L, candidates.take(2), 10)
        val none = WeightedDrawer.draw(5L, emptyList(), 3)

        // then
        assertEquals(setOf(30L, 10L), all.toSet())
        assertEquals(emptyList<Long>(), none)
    }

    @DisplayName("D21 응모권 0인 후보는 뽑지 않는다")
    @Test
    fun skipsZeroTickets() {
        // when
        val winners = WeightedDrawer.draw(9L, listOf(DrawCandidate(1L, 0L), DrawCandidate(2L, 5L)), 2)

        // then
        assertEquals(listOf(2L), winners)
    }

    @DisplayName("응모권이 12배인 후보는 1등으로 뽑히는 비율이 더 높다 (씨앗 2000개 표본)")
    @Test
    fun weightAffectsProbability() {
        // given
        val pair = listOf(DrawCandidate(1L, 12L), DrawCandidate(2L, 1L))

        // when
        val heavyFirst = (1L..2000L).count { seed -> WeightedDrawer.draw(seed, pair, 1).first() == 1L }

        // then
        assertTrue(heavyFirst in 1700..1990, "응모권 12장 후보 1등 횟수=$heavyFirst")
    }
}
