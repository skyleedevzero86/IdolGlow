package com.sleekydz86.idolglow.groundedsearch.application.port

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedCitationPrompt

interface AnswerGeneratePort {
    fun answer(query: String, citations: List<GroundedCitationPrompt>): String?
}
