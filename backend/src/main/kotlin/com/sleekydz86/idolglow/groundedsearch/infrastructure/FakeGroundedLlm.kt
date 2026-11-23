package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.application.port.AnswerGeneratePort
import com.sleekydz86.idolglow.groundedsearch.application.port.ConditionExtractPort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedCitationPrompt
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedConditionDraft
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class FakeGroundedLlm : ConditionExtractPort, AnswerGeneratePort {
    override fun extract(query: String): GroundedConditionDraft = GroundedConditionDraft()

    override fun answer(query: String, citations: List<GroundedCitationPrompt>): String =
        citations.joinToString(" ") { "[${it.label}]" }
}
