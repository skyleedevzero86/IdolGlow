package com.sleekydz86.idolglow.groundedsearch.domain

object ReciprocalRankFusion {
    fun merge(
        keywordIds: List<Long>,
        vectorIds: List<Long>,
        k: Int,
        limit: Int,
    ): List<FusedRank> {
        val keywordRank = ranks(keywordIds)
        val vectorRank = ranks(vectorIds)
        val ids = LinkedHashSet<Long>()
        ids.addAll(keywordIds)
        ids.addAll(vectorIds)
        return ids
            .map { id -> fused(id, keywordRank[id], vectorRank[id], k) }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun ranks(ids: List<Long>): Map<Long, Int> =
        ids.mapIndexed { index, id -> id to index + 1 }.toMap()

    private fun fused(
        chunkId: Long,
        keywordRank: Int?,
        vectorRank: Int?,
        k: Int,
    ): FusedRank = FusedRank(chunkId, keywordRank, vectorRank, score(keywordRank, k) + score(vectorRank, k))

    private fun score(rank: Int?, k: Int): Double = rank?.let { 1.0 / (k + it) } ?: 0.0
}
