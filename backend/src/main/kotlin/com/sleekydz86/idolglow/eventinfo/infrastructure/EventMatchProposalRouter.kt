package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMatchReasonPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchFacts
import com.sleekydz86.idolglow.global.llm.LlmGateway
import com.sleekydz86.idolglow.global.llm.LlmRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EventMatchProposalRouter(
    private val llmGateway: LlmGateway,
) : EventMatchReasonPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun reason(facts: EventMatchFacts): String = modelReason(facts) ?: facts.localReason()

    private fun modelReason(facts: EventMatchFacts): String? {
        val result =
            llmGateway.complete(
                LlmRequest(
                    feature = FEATURE,
                    systemPrompt = SYSTEM,
                    userPrompt = userPrompt(facts),
                    jsonOutput = false,
                    maxOutputTokens = MAX_OUTPUT_TOKENS,
                    temperature = 0.0,
                ),
            ) ?: return null
        val sentence =
            result.text
                .lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() }
                .orEmpty()
        return sentence.takeIf { grounded(it, facts) }.also {
            if (it == null) {
                log.warn("공연 매칭 근거 버림 provider={} text={}", result.provider, sentence.take(LOG_TEXT_LIMIT))
            }
        }
    }

    private fun userPrompt(facts: EventMatchFacts): String =
        "${facts.prompt}\n판정=${facts.verdict()}\n" +
            "제목 같음=${facts.sameNormalizedTitle}, 기간 겹침=${facts.periodsOverlap}, 장소 같음=${facts.sameVenue}"

    private fun grounded(
        sentence: String,
        facts: EventMatchFacts,
    ): Boolean {
        val source = facts.prompt.lowercase()
        return sentence.startsWith(facts.verdict()) &&
            sentence.length <= MAX_SENTENCE_LENGTH &&
            FOREIGN.findAll(sentence).all { source.contains(it.value.lowercase()) } &&
            DATE.findAll(sentence).all { source.contains(it.value) }
    }

    companion object {
        private const val FEATURE: String = "event-match"
        private const val MAX_OUTPUT_TOKENS: Int = 200
        private const val MAX_SENTENCE_LENGTH: Int = 300
        private const val LOG_TEXT_LIMIT: Int = 200
        private val FOREIGN = Regex("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}]+|[A-Za-z][A-Za-z_']+")
        private val DATE = Regex("\\d{4}-\\d{2}-\\d{2}")
        private val SYSTEM: String =
            """
            You explain, in one Korean sentence, why two performance records are judged the same show or not.
            The judgment is already decided by rules and given as 판정. Never contradict it.
            Start the sentence exactly with the given 판정 text, then "근거:" and the reasons, using only the given facts (title, period, venue).
            Copy titles and venue names exactly. No Chinese characters or Japanese unless they appear in the input. No keys, tokens or personal data.
            Example: 같은 공연으로 보임. 근거: 기간이 2026-05-01~2026-05-03으로 같고 장소가 세종문화회관으로 같으며 제목은 부제만 다릅니다.
            """.trimIndent()
    }
}
