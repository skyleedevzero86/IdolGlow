package com.sleekydz86.idolglow.groundedsearch.domain

data class FusedRank(
    val chunkId: Long,
    val keywordRank: Int?,
    val vectorRank: Int?,
    val score: Double,
)
