package com.sleekydz86.idolglow.groundedsearch.adapter.web

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSearchHit

data class GroundedSearchHitResponse(
    val chunkId: Long,
    val sourceType: String,
    val sourceId: Long,
    val linkKey: String,
    val content: String,
    val keywordRank: Int?,
    val vectorRank: Int?,
    val rrfScore: Double,
    val rerankScore: Double?,
) {
    companion object {
        fun from(hit: GroundedSearchHit): GroundedSearchHitResponse =
            GroundedSearchHitResponse(
                chunkId = hit.chunkId,
                sourceType = hit.sourceType.name,
                sourceId = hit.sourceId,
                linkKey = hit.linkKey,
                content = hit.content,
                keywordRank = hit.keywordRank,
                vectorRank = hit.vectorRank,
                rrfScore = hit.rrfScore,
                rerankScore = hit.rerankScore,
            )
    }
}
