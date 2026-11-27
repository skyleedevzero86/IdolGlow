package com.sleekydz86.idolglow.luckydraw.domain

import java.util.Random

data class DrawCandidate(
    val userId: Long,
    val tickets: Long,
)

object WeightedDrawer {
    fun draw(
        seed: Long,
        candidates: List<DrawCandidate>,
        count: Int,
    ): List<Long> {
        require(count >= 0) { "뽑을 수는 0 이상이어야 합니다." }
        val pool = candidates.filter { it.tickets > 0 }.sortedBy { it.userId }.toMutableList()
        val random = Random(seed)
        val winners = mutableListOf<Long>()
        while (winners.size < count && pool.isNotEmpty()) {
            val picked = pick(pool, random.nextLong(pool.sumOf { it.tickets }))
            winners += pool.removeAt(picked).userId
        }
        return winners
    }

    private fun pick(
        pool: List<DrawCandidate>,
        target: Long,
    ): Int {
        var cumulative = 0L
        pool.forEachIndexed { index, candidate ->
            cumulative += candidate.tickets
            if (cumulative > target) return index
        }
        return pool.lastIndex
    }
}
