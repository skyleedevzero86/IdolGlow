package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.EventMatchFacts
import com.sleekydz86.idolglow.global.llm.LlmGateway
import com.sleekydz86.idolglow.global.llm.LlmGatewayProperties
import com.sleekydz86.idolglow.global.llm.LlmInputMasker
import com.sleekydz86.idolglow.global.llm.LlmProvider
import com.sleekydz86.idolglow.global.llm.LlmRequest
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("공연 매칭 근거 라우터")
class EventMatchProposalRouterTest {
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

    private val facts =
        EventMatchFacts(
            sameNormalizedTitle = true,
            periodsOverlap = true,
            sameVenue = false,
            prompt =
                "왼쪽 제목=아이유 콘서트 HEREH 기간=2026-11-20~2026-11-22 장소=KSPO DOME contentId=a3 / " +
                    "오른쪽 제목=아이유 콘서트 HEREH 기간=2026-11-20~2026-11-22 장소=올림픽공원 체조경기장 contentId=b3",
        )

    private fun router(vararg providers: LlmProvider): EventMatchProposalRouter {
        val props =
            LlmGatewayProperties(
                externalEnabled = false,
                chains = mapOf("event-match" to listOf("local", "openai")),
            )
        val gateway =
            LlmGateway(
                providers.toList(),
                props,
                CircuitBreakerRegistry.ofDefaults(),
                SimpleMeterRegistry(),
                LlmInputMasker(),
            )
        return EventMatchProposalRouter(gateway)
    }

    @DisplayName("규칙 판정과 사실을 넘기고, 판정으로 시작하는 근거 문장을 쓴다")
    @Test
    fun usesGroundedModelSentence() {
        val sentence = "같은 공연으로 보임. 근거: 제목과 기간 2026-11-20~2026-11-22가 같고 장소만 KSPO DOME과 올림픽공원 체조경기장으로 다릅니다."
        val local = FakeProvider("local", false, sentence)

        assertEquals(sentence, router(local).reason(facts))
        val prompt = local.requests.single().userPrompt
        assertTrue(prompt.contains("판정=같은 공연으로 보임."))
        assertTrue(prompt.contains("장소 같음=false"))
    }

    @DisplayName("규칙 판정과 반대로 말하면 규칙 문장을 쓴다")
    @Test
    fun fallsBackWhenModelContradictsRule() {
        val local = FakeProvider("local", false, "다른 공연으로 보임. 근거: 장소가 다릅니다.")

        assertEquals(facts.localReason(), router(local).reason(facts))
    }

    @DisplayName("입력에 없는 외국 문자나 날짜를 쓰면 규칙 문장을 쓴다")
    @Test
    fun fallsBackOnUngroundedText() {
        val chinese = "같은 공연으로 보임. 근거: 기간이 같고 场地만 다릅니다."
        val english = "같은 공연으로 보임. 근거: same period, different venue."
        val wrongDate = "같은 공연으로 보임. 근거: 기간이 2026-12-01로 같습니다."

        listOf(chinese, english, wrongDate).forEach { answer ->
            assertEquals(facts.localReason(), router(FakeProvider("local", false, answer)).reason(facts))
        }
    }

    @DisplayName("외부 사용이 꺼져 있으면 로컬이 실패해도 외부를 부르지 않고 규칙 문장을 쓴다")
    @Test
    fun keepsExternalOffByDefault() {
        val local = FakeProvider("local", false, null)
        val openai = FakeProvider("openai", true, "같은 공연으로 보임. 근거: 기간이 같습니다.")

        assertEquals(facts.localReason(), router(local, openai).reason(facts))
        assertTrue(openai.requests.isEmpty())
    }
}
