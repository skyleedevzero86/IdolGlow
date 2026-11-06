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
        private val answer: String?,
    ) : LlmProvider {
        val requests = mutableListOf<LlmRequest>()

        override fun isConfigured(): Boolean = true

        override fun complete(request: LlmRequest): String? {
            requests += request
            return answer
        }
    }

    private val objectMapper = ObjectMapper()

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

    @DisplayName("로컬 응답의 코드펜스를 벗기고 역 요약으로 바꾼다")
    @Test
    fun parsesLocalAnswer() {
        val answer =
            """
            ```json
            {"title":" 강남역 ","bullets":["하나"," 둘 ","셋","넷"]}
            ```
            """.trimIndent()
        val local = FakeProvider("local", false, answer)

        val summary = router(listOf(local)).generate()

        assertEquals("강남역", summary?.title)
        assertEquals(listOf("하나", "둘", "셋"), summary?.bullets)
        assertEquals("강남에 대해 더 알아보세요", summary?.learnMoreLabel)
        assertEquals("subway", local.requests.single().feature)
    }

    @DisplayName("JSON이 아니면 null을 돌려준다")
    @Test
    fun returnsNullWhenAnswerIsNotJson() {
        val local = FakeProvider("local", false, "강남역은 번화가입니다")

        assertNull(router(listOf(local)).generate())
    }

    @DisplayName("지하철 체인이 로컬뿐이면 외부가 켜져 있어도 외부를 부르지 않는다")
    @Test
    fun doesNotCallExternalWhenChainIsLocalOnly() {
        val local = FakeProvider("local", false, null)
        val openai = FakeProvider("openai", true, """{"title":"강남"}""")

        val summary = router(listOf(local, openai), externalEnabled = true).generate()

        assertNull(summary)
        assertTrue(openai.requests.isEmpty())
    }

    @DisplayName("요약 기능이 꺼져 있으면 공급자를 부르지 않는다")
    @Test
    fun skipsWhenSummaryDisabled() {
        val local = FakeProvider("local", false, """{"title":"강남"}""")

        assertNull(router(listOf(local), summaryEnabled = false).generate())
        assertTrue(local.requests.isEmpty())
    }
}
