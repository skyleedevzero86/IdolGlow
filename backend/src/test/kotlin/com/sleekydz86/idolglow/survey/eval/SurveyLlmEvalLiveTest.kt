package com.sleekydz86.idolglow.survey.eval

import com.sleekydz86.idolglow.global.llm.AnthropicLlmProvider
import com.sleekydz86.idolglow.global.llm.LlmProvider
import com.sleekydz86.idolglow.global.llm.LlmRequest
import com.sleekydz86.idolglow.global.llm.LocalLlmProperties
import com.sleekydz86.idolglow.global.llm.LocalLlmProvider
import com.sleekydz86.idolglow.global.llm.OpenAiLlmProvider
import com.sleekydz86.idolglow.survey.infrastructure.SurveyLlmPrompts
import com.sleekydz86.idolglow.survey.infrastructure.SurveyRecommendationValidator
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper
import kotlin.system.measureTimeMillis

@Tag("llm-eval")
@DisplayName("설문 추천 LLM 실시간 평가")
class SurveyLlmEvalLiveTest {
    private val objectMapper = ObjectMapper()
    private val scorer = SurveyLlmEvalScorer(SurveyRecommendationValidator(objectMapper))
    private val webClient = WebClient.builder().build()

    @Test
    @DisplayName("공급자별 설문 추천 품질을 평가하고 보고서를 만든다")
    fun evaluatesProviders() {
        val cases = loadCases()
        val results = targets().map { evaluate(it, cases) }

        SurveyLlmEvalReport.write(results)

        results.filter { it.skippedReason == null }.forEach { result ->
            val successRate = result.samples.count { it.score.formatSuccess }.toDouble() / result.samples.size
            assertTrue(successRate >= MIN_FORMAT_SUCCESS_RATE, "${result.provider} 형식 성공률이 기준보다 낮습니다")
        }
    }

    private fun evaluate(
        target: ProviderTarget,
        cases: List<SurveyEvalCase>,
    ): SurveyEvalProviderResult {
        val provider = target.provider ?: return skipped(target, target.unavailableReason)
        val warmUpError = runCatching { provider.complete(request(cases.first())) }.exceptionOrNull()
        return if (warmUpError != null) {
            skipped(target, "워밍업 실패: ${firstLine(warmUpError)}")
        } else {
            val samples = cases.mapIndexed { index, case -> sample(provider, index, case) }
            SurveyEvalProviderResult(target.label, samples, cost = target.cost)
        }
    }

    private fun sample(
        provider: LlmProvider,
        index: Int,
        case: SurveyEvalCase,
    ): SurveyEvalSampleResult {
        var response: Result<String?> = Result.success(null)
        val latency = measureTimeMillis { response = runCatching { provider.complete(request(case)) } }
        val score =
            response.fold(
                onSuccess = { text -> text?.let { scorer.score(it, case) } ?: failed("응답 없음") },
                onFailure = { failed("응답 오류: ${firstLine(it)}") },
            )
        return SurveyEvalSampleResult(index + SAMPLE_NUMBER_OFFSET, latency, score, response.getOrNull())
    }

    private fun failed(reason: String) = SurveyEvalScore(false, false, false, listOf(reason))

    private fun firstLine(error: Throwable): String {
        val message = error.message?.lineSequence()?.firstOrNull()
        return message ?: error.javaClass.simpleName
    }

    private fun request(case: SurveyEvalCase): LlmRequest =
        LlmRequest(
            feature = FEATURE,
            systemPrompt = SurveyLlmPrompts.system,
            userPrompt =
                SurveyLlmPrompts.userJson(
                    objectMapper,
                    case.answerHighlights,
                    case.attractions.map { it.domain() },
                ),
            maxOutputTokens = MAX_OUTPUT_TOKENS,
            temperature = TEMPERATURE,
        )

    private fun targets(): List<ProviderTarget> {
        val openAiKey = System.getenv(OPENAI_KEY).orEmpty()
        val anthropicKey = System.getenv(ANTHROPIC_KEY).orEmpty()
        return listOf(
            localTarget("local qwen2.5:3b", "qwen2.5:3b"),
            localTarget("local qwen2.5:7b", "qwen2.5:7b"),
            ProviderTarget(
                "OpenAI",
                openAiKey.takeIf { it.isNotBlank() }?.let {
                    OpenAiLlmProvider(webClient, objectMapper, true, OPENAI_BASE_URL, it, OPENAI_MODEL)
                },
                "입출력 토큰 미수집",
                "$OPENAI_KEY 없음",
            ),
            ProviderTarget(
                "Anthropic",
                anthropicKey.takeIf { it.isNotBlank() }?.let {
                    AnthropicLlmProvider(webClient, objectMapper, true, ANTHROPIC_BASE_URL, it, ANTHROPIC_MODEL)
                },
                "입출력 토큰 미수집",
                "$ANTHROPIC_KEY 없음",
            ),
        )
    }

    private fun localTarget(
        label: String,
        model: String,
    ): ProviderTarget =
        ProviderTarget(
            label,
            LocalLlmProvider(
                webClient,
                objectMapper,
                LocalLlmProperties(true, ollamaBaseUrl(), model, LOCAL_TIMEOUT_SECONDS),
            ),
            "0원",
            "Ollama 또는 모델을 사용할 수 없음",
        )

    private fun ollamaBaseUrl(): String = System.getenv(OLLAMA_URL_ENV)?.takeIf { it.isNotBlank() } ?: OLLAMA_BASE_URL

    private fun skipped(
        target: ProviderTarget,
        reason: String,
    ) = SurveyEvalProviderResult(target.label, emptyList(), reason, target.cost)

    private fun loadCases(): List<SurveyEvalCase> {
        val stream = requireNotNull(javaClass.getResourceAsStream(CASES_RESOURCE))
        return stream.use {
            val type = objectMapper.typeFactory.constructCollectionType(List::class.java, SurveyEvalCase::class.java)
            objectMapper.readValue(it, type)
        }
    }

    private data class ProviderTarget(
        val label: String,
        val provider: LlmProvider?,
        val cost: String,
        val unavailableReason: String,
    )

    companion object {
        private const val FEATURE: String = "survey"
        private const val MAX_OUTPUT_TOKENS: Int = 900
        private const val TEMPERATURE: Double = 0.3
        private const val MIN_FORMAT_SUCCESS_RATE: Double = 0.9
        private const val SAMPLE_NUMBER_OFFSET: Int = 1
        private const val LOCAL_TIMEOUT_SECONDS: Long = 120
        private const val OLLAMA_URL_ENV: String = "OLLAMA_URL"
        private const val OLLAMA_BASE_URL: String = "http://localhost:11434/v1"
        private const val OPENAI_BASE_URL: String = "https://api.openai.com"
        private const val ANTHROPIC_BASE_URL: String = "https://api.anthropic.com"
        private const val OPENAI_MODEL: String = "gpt-4.1-mini"
        private const val ANTHROPIC_MODEL: String = "claude-3-5-haiku-20241022"
        private const val OPENAI_KEY: String = "OPENAI_API_KEY"
        private const val ANTHROPIC_KEY: String = "ANTHROPIC_API_KEY"
        private const val CASES_RESOURCE: String = "/survey/eval/cases.json"
    }
}
