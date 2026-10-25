package com.sleekydz86.idolglow.groundedsearch.adapter.web

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedAnswer
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedCitationPrompt

data class GroundedAnswerResponse(
    val answer: String?,
    val citations: List<GroundedCitationResponse>,
    val products: List<GroundedProductAnswerResponse>,
    val pipeline: GroundedPipelineResponse,
) {
    companion object {
        fun from(answer: GroundedAnswer): GroundedAnswerResponse =
            GroundedAnswerResponse(
                answer = answer.answer,
                citations = answer.citations.map(::citation),
                products = answer.products.map(GroundedProductAnswerResponse::from),
                pipeline = GroundedPipelineResponse.from(answer),
            )

        private fun citation(prompt: GroundedCitationPrompt): GroundedCitationResponse =
            GroundedCitationResponse(prompt.label, prompt.chunkId, prompt.linkKey, prompt.content)
    }
}
