package com.sleekydz86.idolglow.survey.infrastructure

import com.sleekydz86.idolglow.global.llm.LlmGateway
import com.sleekydz86.idolglow.global.llm.LlmGatewayProperties
import com.sleekydz86.idolglow.global.llm.LlmInputMasker
import com.sleekydz86.idolglow.global.llm.LlmProvider
import com.sleekydz86.idolglow.global.llm.LlmRequest
import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

@DisplayName("설문 추천 라우터")
class SurveyRecommendationLlmRouterTest {
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

    private val attractions =
        listOf(
            attraction("A1", "경복궁"),
            attraction("A2", "N서울타워"),
        )

    private fun attraction(
        code: String,
        name: String,
    ) = TourAttraction(code, name, 1, "서울", 11530, "종로구", "관광지", "역사관광지", 1, null, null, "202610")

    private fun router(vararg providers: LlmProvider): SurveyRecommendationLlmRouter {
        val props =
            LlmGatewayProperties(externalEnabled = false, chains = mapOf("survey" to listOf("local", "openai")))
        val gateway =
            LlmGateway(
                providers.toList(),
                props,
                CircuitBreakerRegistry.ofDefaults(),
                SimpleMeterRegistry(),
                LlmInputMasker(),
            )
        return SurveyRecommendationLlmRouter(gateway, objectMapper)
    }

    private fun answer(
        title: String = "BTS 팬을 위한 서울 3박 4일",
        narrative: String = "경복궁과 N서울타워를 함께 둘러보세요. 전통과 야경을 모두 즐길 수 있습니다.",
        reasons: Map<String, String> = mapOf("A1" to "전통 문화를 느낄 수 있습니다.", "A2" to "서울 풍경을 볼 수 있습니다."),
    ) = objectMapper.writeValueAsString(
        mapOf("title" to title, "subtitle" to "전통과 야경", "narrative" to narrative, "attractionReasons" to reasons),
    )

    private fun generate(router: SurveyRecommendationLlmRouter) = router.generate(listOf("BTS", "3박 4일"), attractions)

    @DisplayName("채울 틀과 관광지 목록을 넘기고, 목록에 있는 관광지 이유만 남긴다")
    @Test
    fun keepsOnlyListedAttractionReasons() {
        val reasons = mapOf("A1" to "전통 문화를 느낄 수 있습니다.", "X9" to "목록에 없는 곳입니다.")
        val local = FakeProvider("local", false, answer(reasons = reasons))

        val result = generate(router(local))

        assertEquals(setOf("A1"), result?.attractionReasons?.keys)
        val prompt = local.requests.single().userPrompt
        assertTrue(prompt.contains("fillThis") && prompt.contains("\"A2\":\"\""))
    }

    @DisplayName("한자·가나·키릴 문자나 입력에 없는 영단어가 나오면 버린다")
    @Test
    fun dropsForeignOrUngroundedWords() {
        val cyrillic = answer(reasons = mapOf("A1" to "Современное 문화를 즐기세요."))
        val chinese = answer(narrative = "经典 코스입니다.")
        val invented = answer(narrative = "경복궁 근처 Starbucks에서 쉬어 가세요.")

        listOf(cyrillic, chinese, invented).forEach { text ->
            assertNull(generate(router(FakeProvider("local", false, text))))
        }
    }

    @DisplayName("입력에 있는 영문(BTS, N서울타워)은 허용한다")
    @Test
    fun allowsLatinWordsFromInput() {
        val local = FakeProvider("local", false, answer())

        assertEquals("BTS 팬을 위한 서울 3박 4일", generate(router(local))?.title)
    }

    @DisplayName("외부 사용이 꺼져 있으면 로컬이 실패해도 외부를 부르지 않는다")
    @Test
    fun keepsExternalOffByDefault() {
        val local = FakeProvider("local", false, null)
        val openai = FakeProvider("openai", true, answer())

        assertNull(generate(router(local, openai)))
        assertTrue(openai.requests.isEmpty())
    }
}
