package com.sleekydz86.idolglow.groundedsearch.domain

data class GroundedAnswer(
    val answer: String?,
    val citations: List<GroundedCitationPrompt>,
    val products: List<GroundedProductFacts>,
    val extracted: GroundedChecked,
    val relaxed: List<String>,
    val keywordHits: List<Long>,
    val vectorHits: List<Long>,
    val rrf: List<FusedRank>,
    val rerankScores: List<Double?>,
    val verdict: GroundedVerdict,
)
