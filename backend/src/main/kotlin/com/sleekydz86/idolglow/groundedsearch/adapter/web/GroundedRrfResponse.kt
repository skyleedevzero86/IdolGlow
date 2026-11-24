package com.sleekydz86.idolglow.groundedsearch.adapter.web

import com.sleekydz86.idolglow.groundedsearch.domain.FusedRank

data class GroundedRrfResponse(
    val chunkId: Long,
    val keywordRank: Int?,
    val vectorRank: Int?,
    val score: Double,
) {
    companion object {
        fun from(rank: FusedRank): GroundedRrfResponse =
            GroundedRrfResponse(rank.chunkId, rank.keywordRank, rank.vectorRank, rank.score)
    }
}
