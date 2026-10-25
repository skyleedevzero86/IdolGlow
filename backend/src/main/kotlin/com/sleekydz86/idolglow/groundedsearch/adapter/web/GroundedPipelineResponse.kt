package com.sleekydz86.idolglow.groundedsearch.adapter.web

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedAnswer

data class GroundedPipelineResponse(
    val extracted: GroundedExtractedResponse,
    val ignored: List<String>,
    val relaxed: List<String>,
    val keywordHits: List<Long>,
    val vectorHits: List<Long>,
    val rrf: List<GroundedRrfResponse>,
    val rerankScores: List<Double?>,
    val validation: GroundedValidationResponse,
) {
    companion object {
        fun from(answer: GroundedAnswer): GroundedPipelineResponse =
            GroundedPipelineResponse(
                extracted = GroundedExtractedResponse.from(answer.extracted),
                ignored = answer.extracted.ignored,
                relaxed = answer.relaxed,
                keywordHits = answer.keywordHits,
                vectorHits = answer.vectorHits,
                rrf = answer.rrf.map(GroundedRrfResponse::from),
                rerankScores = answer.rerankScores,
                validation = GroundedValidationResponse.from(answer.verdict),
            )
    }
}
