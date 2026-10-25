package com.sleekydz86.idolglow.groundedsearch.domain

data class GroundedRetrieval(
    val hits: List<GroundedSearchHit>,
    val keywordHits: List<Long>,
    val vectorHits: List<Long>,
    val fused: List<FusedRank>,
)
