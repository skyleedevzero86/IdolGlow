package com.sleekydz86.idolglow.translation.eval

import com.sleekydz86.idolglow.translation.application.port.out.EngineRequest
import com.sleekydz86.idolglow.translation.application.port.out.JudgeRequest
import com.sleekydz86.idolglow.translation.domain.GuardInput
import com.sleekydz86.idolglow.translation.domain.TranslationGlossaryTerm
import com.sleekydz86.idolglow.translation.domain.TranslationGuard
import com.sleekydz86.idolglow.translation.domain.TranslationProtector
import com.sleekydz86.idolglow.translation.infrastructure.LlmTranslationEngine
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@ActiveProfiles("eval")
@Tag("eval")
@EnabledIfEnvironmentVariable(named = "LLM_LOCAL_ENABLED", matches = "true")
@DisplayName("번역 품질 평가")
class TranslationEvalLiveTest {
    @Autowired
    private lateinit var engine: LlmTranslationEngine

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("로컬 모델로 평가셋을 번역해 검사 통과율과 채점 점수를 잰다")
    fun measuresPassRateWithLocalModel() {
        // given
        val cases = loadCases()

        // when
        val results = cases.map(::run)
        val passed = results.count { it.passed }
        val passRate = passed.toDouble() / results.size
        println("번역 평가 결과: 통과 $passed/${results.size} (${(passRate * PERCENT).toInt()}%)")
        results.forEach { println(" - ${it.id}: ${if (it.passed) "통과" else "실패"} judge=${it.judge} ${it.problems}") }

        // then
        assertTrue(passRate >= MIN_PASS_RATE) {
            "검사 통과율이 ${(MIN_PASS_RATE * PERCENT).toInt()}% 미만입니다. 통과 $passed/${results.size}"
        }
    }

    private fun run(case: EvalCase): EvalResult {
        val protectedFields = case.fields.mapValues { TranslationProtector.protect(it.value) }
        val request = EngineRequest(case.locale, protectedFields.mapValues { it.value.text }, case.glossary)
        val response = engine.translate(request)
        val raw = response?.fields ?: return EvalResult(case.id, false, null, listOf("engine_or_parse_failed"))
        val guard =
            TranslationGuard.check(
                GuardInput(
                    locale = case.locale,
                    source = protectedFields.mapValues { it.value.text },
                    translated = raw,
                    tokenCounts = protectedFields.mapValues { it.value.tokens.size },
                    glossary = case.glossary,
                ),
            )
        val restored =
            raw
                .filterKeys { it in protectedFields }
                .mapValues { (field, value) ->
                    TranslationProtector.restore(value, protectedFields.getValue(field).tokens)
                }
        val joined = restored.values.joinToString("\n")
        val missing = case.mustContain.filter { !joined.contains(it) }.map { "must_contain:$it" }
        val judge = engine.judge(JudgeRequest(case.locale, case.fields, restored))
        val problems = guard.hard + guard.soft + missing
        return EvalResult(case.id, problems.isEmpty(), judge, problems)
    }

    private fun loadCases(): List<EvalCase> {
        val stream = requireNotNull(javaClass.getResourceAsStream(CASES)) { "평가셋 파일이 없습니다. path=$CASES" }
        return stream
            .bufferedReader(Charsets.UTF_8)
            .readLines()
            .filter { it.isNotBlank() }
            .map { line -> toCase(objectMapper.readTree(line)) }
    }

    private fun toCase(node: JsonNode): EvalCase =
        EvalCase(
            id = node.path("id").asText(),
            locale = node.path("locale").asText(),
            fields = node.path("fields").properties().associate { (key, value) -> key to value.asText() },
            mustContain = node.path("mustContain").toList().map { it.asText() },
            glossary =
                node.path("glossary").toList().map { term ->
                    if (term.has("keep")) {
                        TranslationGlossaryTerm.keep(term.path("keep").asText())
                    } else {
                        TranslationGlossaryTerm.fixed(
                            term.path("source").asText(),
                            node.path("locale").asText(),
                            term.path("target").asText(),
                        )
                    }
                },
        )

    private data class EvalCase(
        val id: String,
        val locale: String,
        val fields: Map<String, String>,
        val mustContain: List<String>,
        val glossary: List<TranslationGlossaryTerm>,
    )

    private data class EvalResult(
        val id: String,
        val passed: Boolean,
        val judge: Int?,
        val problems: List<String>,
    )

    companion object {
        private const val CASES = "/translation/eval/cases.jsonl"
        private const val MIN_PASS_RATE = 0.8
        private const val PERCENT = 100
    }
}
