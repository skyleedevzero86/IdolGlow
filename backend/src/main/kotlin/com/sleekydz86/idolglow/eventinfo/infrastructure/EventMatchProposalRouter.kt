package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMatchReasonPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchFacts
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class EventMatchProposalRouter(
    private val openAiEventMatchReasonClient: OpenAiEventMatchReasonClient,
    private val anthropicEventMatchReasonClient: AnthropicEventMatchReasonClient,
    @Value("\${app.llm.event-match.provider:\${app.llm.survey.provider:auto}}")
    private val providerRaw: String,
) : EventMatchReasonPort {
    override fun reason(facts: EventMatchFacts): String {
        val model = modelReason(facts.prompt)?.trim()?.takeIf { it.isNotEmpty() }
        return model ?: facts.localReason()
    }

    private fun modelReason(prompt: String): String? {
        val provider = providerRaw.trim().lowercase()
        return when (provider) {
            "openai", "gpt" -> openAiEventMatchReasonClient.reason(prompt)
            "anthropic", "claude" -> anthropicEventMatchReasonClient.reason(prompt)
            else ->
                openAiEventMatchReasonClient.reason(prompt)
                    ?: anthropicEventMatchReasonClient.reason(prompt)
        }
    }
}
