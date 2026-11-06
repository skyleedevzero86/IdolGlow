package com.sleekydz86.idolglow.subway.infrastructure.enrichment

import com.sleekydz86.idolglow.global.llm.LlmGateway
import com.sleekydz86.idolglow.global.llm.LlmGatewayProperties
import com.sleekydz86.idolglow.global.llm.LlmInputMasker
import com.sleekydz86.idolglow.global.llm.LlmProvider
import com.sleekydz86.idolglow.global.llm.LlmRequest
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

@DisplayName("지하철역 요약 라우터")
class SubwayStationSummaryLlmRouterTest {
    private class FakeProvider(
        override val name: String,
        override val external: Boolean,
        answers: List<String?>,
    ) : LlmProvider {
        private val queue = ArrayDeque(answers)
        val requests = mutableListOf<LlmRequest>()

        override fun isConfigured(): Boolean = true

        override fun complete(request: LlmRequest): String? {
            requests += request
            return queue.removeFirstOrNull()
        }
    }

    private val objectMapper = ObjectMapper()

    private val english = """{"bullets":["Busy business area.","Many shops nearby.","May be crowded at rush hour."]}"""

    private fun router(
        providers: List<LlmProvider>,
        chain: List<String> = listOf("local"),
        externalEnabled: Boolean = false,
        summaryEnabled: Boolean = true,
    ): SubwayStationSummaryLlmRouter {
        val props = LlmGatewayProperties(externalEnabled = externalEnabled, chains = mapOf("subway" to chain))
        val gateway =
            LlmGateway(providers, props, CircuitBreakerRegistry.ofDefaults(), SimpleMeterRegistry(), LlmInputMasker())
        return SubwayStationSummaryLlmRouter(gateway, objectMapper, summaryEnabled)
    }

    private fun SubwayStationSummaryLlmRouter.generate() = generateSummary("2", "2호선", "222", "강남")

    @DisplayName("영어로 만든 문장을 한국어로 번역해 문장만 돌려준다")
    @Test
    fun writesInEnglishThenTranslates() {
        val korean = "```json\n{\"bullets\":[\" 업무 지역입니다. \",\"상점이 많습니다.\",\"출퇴근 때 붐빌 수 있습니다.\"]}\n```"
        val local = FakeProvider("local", false, listOf(english, korean))

        val summary = router(listOf(local)).generate()

        assertEquals(listOf("업무 지역입니다.", "상점이 많습니다.", "출퇴근 때 붐빌 수 있습니다."), summary?.bullets)
        assertNull(summary?.title)
        assertNull(summary?.learnMoreLabel)
        assertTrue(local.requests[1].userPrompt.contains("Busy business area."))
    }

    @DisplayName("번역에 한자, 가나, 영문이 섞이면 버린다")
    @Test
    fun dropsTranslationWithNonKoreanLetters() {
        val chinese = """{"bullets":["업무 지역입니다.","周边设施完善","붐빌 수 있습니다."]}"""
        val japanese = """{"bullets":["ビジネ스 중심","상점이 많습니다.","붐빌 수 있습니다."]}"""
        val latin = """{"bullets":["업무 지역입니다.","용hill 캠퍼스가 가깝습니다.","붐빌 수 있습니다."]}"""

        listOf(chinese, japanese, latin).forEach { translated ->
            assertNull(router(listOf(FakeProvider("local", false, listOf(english, translated)))).generate())
        }
    }

    @DisplayName("영어 문장이 3개가 아니거나 JSON이 아니면 번역을 부르지 않는다")
    @Test
    fun skipsTranslationWhenEnglishIsInvalid() {
        val twoBullets = FakeProvider("local", false, listOf("""{"bullets":["One.","Two."]}"""))
        val notJson = FakeProvider("local", false, listOf("Gangnam is busy."))

        assertNull(router(listOf(twoBullets)).generate())
        assertNull(router(listOf(notJson)).generate())
        assertEquals(1, twoBullets.requests.size)
        assertEquals(1, notJson.requests.size)
    }

    @DisplayName("지하철 체인이 로컬뿐이면 외부가 켜져 있어도 외부를 부르지 않는다")
    @Test
    fun doesNotCallExternalWhenChainIsLocalOnly() {
        val local = FakeProvider("local", false, listOf(null))
        val openai = FakeProvider("openai", true, listOf(english))

        assertNull(router(listOf(local, openai), externalEnabled = true).generate())
        assertTrue(openai.requests.isEmpty())
    }

    @DisplayName("요약 기능이 꺼져 있으면 공급자를 부르지 않는다")
    @Test
    fun skipsWhenSummaryDisabled() {
        val local = FakeProvider("local", false, listOf(english))

        assertNull(router(listOf(local), summaryEnabled = false).generate())
        assertTrue(local.requests.isEmpty())
    }
}
