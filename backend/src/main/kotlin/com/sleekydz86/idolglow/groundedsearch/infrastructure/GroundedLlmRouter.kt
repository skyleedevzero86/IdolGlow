package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.application.port.AnswerGeneratePort
import com.sleekydz86.idolglow.groundedsearch.application.port.ConditionExtractPort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedCitationPrompt
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedConditionDraft
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@Component
@Profile("!test")
class GroundedLlmRouter(
    private val openAi: GroundedOpenAiGateway,
    private val anthropic: GroundedAnthropicGateway,
    private val settings: GroundedRemoteSettings,
    private val objectMapper: ObjectMapper,
) : ConditionExtractPort, AnswerGeneratePort {
    override fun extract(query: String): GroundedConditionDraft =
        parse(complete(GroundedLlmPrompts.EXTRACT_SYSTEM, GroundedLlmPrompts.extractUser(query), true))

    override fun answer(query: String, citations: List<GroundedCitationPrompt>): String? =
        complete(GroundedLlmPrompts.ANSWER_SYSTEM, GroundedLlmPrompts.answerUser(query, citations), false)

    private fun complete(system: String, user: String, json: Boolean): String? =
        when (settings.text(PROVIDER).lowercase()) {
            "anthropic", "claude" -> anthropic.complete(system, user)
            "openai", "gpt" -> openAi.complete(system, user, json)
            else -> openAi.complete(system, user, json) ?: anthropic.complete(system, user)
        }

    private fun parse(text: String?): GroundedConditionDraft {
        val node = text?.let { raw -> runCatching { objectMapper.readTree(strip(raw)) }.getOrNull() }
        if (node == null) {
            return GroundedConditionDraft(failed = true)
        }
        return GroundedConditionDraft(
            region = textOf(node, "region"),
            dateFrom = textOf(node, "dateFrom"),
            dateTo = textOf(node, "dateTo"),
            maxBudget = textOf(node, "maxBudget"),
            category = textOf(node, "category"),
            people = textOf(node, "people"),
        )
    }

    private fun strip(raw: String): String =
        raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

    private fun textOf(node: JsonNode, field: String): String? =
        node.path(field).takeUnless { it.isMissingNode || it.isNull }?.asText()?.trim()?.ifBlank { null }

    private companion object {
        private const val PROVIDER = "app.grounded-search.llm.provider"
    }
}
