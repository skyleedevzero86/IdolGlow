package com.sleekydz86.idolglow.subway.infrastructure.enrichment

import com.sleekydz86.idolglow.global.llm.LlmGateway
import com.sleekydz86.idolglow.global.llm.LlmGatewayProperties
import com.sleekydz86.idolglow.global.llm.LlmInputMasker
import com.sleekydz86.idolglow.global.llm.LlmProvider
import com.sleekydz86.idolglow.global.llm.LlmRequest
import com.sleekydz86.idolglow.subway.domain.SubwayLine
import com.sleekydz86.idolglow.subway.domain.SubwayNearbyPlace
import com.sleekydz86.idolglow.subway.domain.SubwayStationLocation
import com.sleekydz86.idolglow.subway.domain.SubwayStationStop
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

    private val lines = listOf(SubwayLine("2", "2호선", "#00A84D"), SubwayLine("AREX", "공항철도", "#0099D8"))
    private val stops =
        mapOf(
            "2" to listOf(stop("0238", "합정"), stop("0239", "홍대입구"), stop("0240", "신촌")),
            "AREX" to listOf(stop("A01", "홍대입구")),
        )
    private val places =
        listOf(
            SubwayNearbyPlace("서울 인디스페이스", "문화시설", 152),
            SubwayNearbyPlace("CGV 홍대", "문화시설", 360),
        )

    private fun stop(
        code: String,
        name: String,
    ) = SubwayStationStop(code = code, name = name, frCode = code)

    private fun router(
        providers: List<LlmProvider>,
        nearby: List<SubwayNearbyPlace> = places,
        externalEnabled: Boolean = false,
        summaryEnabled: Boolean = true,
    ): SubwayStationSummaryLlmRouter {
        val props = LlmGatewayProperties(externalEnabled = externalEnabled, chains = mapOf("subway" to listOf("local")))
        val gateway =
            LlmGateway(providers, props, CircuitBreakerRegistry.ofDefaults(), SimpleMeterRegistry(), LlmInputMasker())
        val assembler =
            SubwayStationFactsAssembler(
                lineCatalogPort = { lines },
                stationOrderPort = { stops[it].orEmpty() },
                locationPort = { SubwayStationLocation(37.557, 126.924) },
                nearbyPlacePort = { nearby },
            )
        return SubwayStationSummaryLlmRouter(gateway, objectMapper, assembler, summaryEnabled)
    }

    private fun SubwayStationSummaryLlmRouter.generate() = generateSummary("2", "2호선", "0239", "홍대입구")

    private fun answer(vararg bullets: String) = objectMapper.writeValueAsString(mapOf("bullets" to bullets.toList()))

    @DisplayName("환승·이웃 역·주변 장소를 프롬프트에 넣고, 사실과 맞는 문장을 돌려준다")
    @Test
    fun returnsGroundedSentences() {
        val local =
            FakeProvider(
                "local",
                false,
                listOf(
                    answer(
                        "2호선 홍대입구역은 공항철도로 갈아탈 수 있고 이웃 역은 합정과 신촌입니다.",
                        "서울 인디스페이스가 약 150m 거리에 있습니다.",
                        "CGV 홍대도 약 400m로 가깝습니다.",
                    ),
                ),
            )

        val summary = router(listOf(local)).generate()

        assertEquals(3, summary?.bullets?.size)
        val prompt = local.requests.single().userPrompt
        assertTrue(prompt.contains("공항철도"))
        assertTrue(prompt.contains("서울 인디스페이스"))
        assertTrue(prompt.contains("합정") && prompt.contains("신촌"))
    }

    @DisplayName("사실에 없는 거리를 쓰면 버린다")
    @Test
    fun dropsInventedDistance() {
        val local =
            FakeProvider(
                "local",
                false,
                listOf(answer("2호선 역입니다.", "서울 인디스페이스가 약 900m 거리에 있습니다.", "CGV 홍대도 가깝습니다.")),
            )

        assertNull(router(listOf(local)).generate())
    }

    @DisplayName("한자·가나·영단어가 섞이면 버리지만 장소 이름의 영문은 허용한다")
    @Test
    fun dropsForeignLettersOutsideNames() {
        val chinese = answer("2호선 역입니다.", "周边 서울 인디스페이스가 있습니다.", "CGV 홍대도 있습니다.")
        val english = answer("2호선 역입니다.", "서울 인디스페이스 is near.", "CGV 홍대도 있습니다.")

        listOf(chinese, english).forEach { text ->
            assertNull(router(listOf(FakeProvider("local", false, listOf(text)))).generate())
        }
    }

    @DisplayName("주변 장소가 없으면 LLM을 부르지 않는다")
    @Test
    fun skipsLlmWithoutNearbyPlaces() {
        val local = FakeProvider("local", false, listOf(answer("a", "b", "c")))

        assertNull(router(listOf(local), nearby = emptyList()).generate())
        assertTrue(local.requests.isEmpty())
    }

    @DisplayName("지하철 체인이 로컬뿐이면 외부가 켜져 있어도 외부를 부르지 않는다")
    @Test
    fun doesNotCallExternalWhenChainIsLocalOnly() {
        val local = FakeProvider("local", false, listOf(null))
        val openai = FakeProvider("openai", true, listOf(answer("a", "b", "c")))

        assertNull(router(listOf(local, openai), externalEnabled = true).generate())
        assertTrue(openai.requests.isEmpty())
    }

    @DisplayName("요약 기능이 꺼져 있으면 공급자를 부르지 않는다")
    @Test
    fun skipsWhenSummaryDisabled() {
        val local = FakeProvider("local", false, listOf(answer("a", "b", "c")))

        assertNull(router(listOf(local), summaryEnabled = false).generate())
        assertTrue(local.requests.isEmpty())
    }
}
