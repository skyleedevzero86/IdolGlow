package com.sleekydz86.idolglow.groundedsearch.domain

data class GroundedSearchHit(
    val chunkId: Long,
    val sourceType: RagSourceType,
    val sourceId: Long,
    val linkKey: String,
    val content: String,
    val keywordRank: Int?,
    val vectorRank: Int?,
    val rrfScore: Double,
)
