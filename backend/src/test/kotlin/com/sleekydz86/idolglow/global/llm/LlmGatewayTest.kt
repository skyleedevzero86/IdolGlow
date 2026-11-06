package com.sleekydz86.idolglow.global.llm

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@DisplayName("LLM 게이트웨이")
class LlmGatewayTest {
    private class FakeProvider(
        override val name: String,
        override val external: Boolean,
        private val answer: () -> String?,
    ) : LlmProvider {
        val prompts = mutableListOf<String>()

        override fun isConfigured(): Boolean = true

        override fun complete(request: LlmRequest): String? {
            prompts += request.userPrompt
            return answer()
        }
    }

    private val request = LlmRequest(feature = "subway", systemPrompt = "system", userPrompt = "문의 user@example.com")

    private fun gateway(
        providers: List<LlmProvider>,
        props: LlmGatewayProperties,
        breakers: CircuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults(),
    ) = LlmGateway(providers, props, breakers, SimpleMeterRegistry(), LlmInputMasker())

    @DisplayName("로컬이 실패하면 체인의 다음 공급자로 넘어간다")
    @Test
    fun fallsBackToNextProviderWhenLocalFails() {
        val local = FakeProvider("local", false) { throw IllegalStateException("down") }
        val openai = FakeProvider("openai", true) { "{\"ok\":true}" }
        val props = LlmGatewayProperties(externalEnabled = true, chains = mapOf("subway" to listOf("local", "openai")))

        val result = gateway(listOf(local, openai), props).complete(request)

        assertEquals("openai", result?.provider)
        assertEquals("{\"ok\":true}", result?.text)
    }

    @DisplayName("외부 사용이 꺼져 있으면 외부 공급자를 부르지 않는다")
    @Test
    fun skipsExternalProviderWhenDisabled() {
        val local = FakeProvider("local", false) { null }
        val openai = FakeProvider("openai", true) { "answer" }
        val props = LlmGatewayProperties(externalEnabled = false, chains = mapOf("subway" to listOf("local", "openai")))

        val result = gateway(listOf(local, openai), props).complete(request)

        assertNull(result)
        assertTrue(openai.prompts.isEmpty())
    }

    @DisplayName("외부 호출은 하루 한도를 넘지 않고 날짜가 바뀌면 다시 허용된다")
    @Test
    fun enforcesDailyExternalLimit() {
        val openai = FakeProvider("openai", true) { "answer" }
        val props =
            LlmGatewayProperties(
                externalEnabled = true,
                externalDailyLimit = 2,
                chains = mapOf("subway" to listOf("openai")),
            )
        val gateway = gateway(listOf(openai), props)
        gateway.clock = Clock.fixed(Instant.parse("2026-11-07T01:00:00Z"), ZoneId.of("Asia/Seoul"))

        repeat(2) { assertEquals("openai", gateway.complete(request)?.provider) }
        assertNull(gateway.complete(request))

        gateway.clock = Clock.fixed(Instant.parse("2026-11-08T01:00:00Z"), ZoneId.of("Asia/Seoul"))
        assertEquals("openai", gateway.complete(request)?.provider)
    }

    @DisplayName("외부 공급자에는 개인정보를 가린 프롬프트를 보낸다")
    @Test
    fun masksPromptForExternalProvider() {
        val local = FakeProvider("local", false) { null }
        val openai = FakeProvider("openai", true) { "answer" }
        val props = LlmGatewayProperties(externalEnabled = true, chains = mapOf("subway" to listOf("local", "openai")))

        gateway(listOf(local, openai), props).complete(request)

        assertEquals("문의 user@example.com", local.prompts.single())
        assertEquals("문의 <email>", openai.prompts.single())
    }

    @DisplayName("서킷이 열리면 그 공급자를 부르지 않고 다음으로 넘어간다")
    @Test
    fun skipsProviderWhenCircuitIsOpen() {
        val breakers =
            CircuitBreakerRegistry.of(
                CircuitBreakerConfig
                    .custom()
                    .slidingWindowSize(2)
                    .minimumNumberOfCalls(2)
                    .failureRateThreshold(50f)
                    .build(),
            )
        var localCalls = 0
        val local = FakeProvider("local", false) {
            localCalls++
            throw IllegalStateException("down")
        }
        val openai = FakeProvider("openai", true) { "answer" }
        val props = LlmGatewayProperties(externalEnabled = true, chains = mapOf("subway" to listOf("local", "openai")))
        val gateway = gateway(listOf(local, openai), props, breakers)

        repeat(3) { gateway.complete(request) }

        assertEquals(2, localCalls)
        assertEquals("openai", gateway.complete(request)?.provider)
    }

    @DisplayName("체인에 없는 기능은 default 체인을 쓴다")
    @Test
    fun usesDefaultChainForUnknownFeature() {
        val local = FakeProvider("local", false) { "local answer" }
        val props = LlmGatewayProperties(chains = mapOf("default" to listOf("local")))

        val result = gateway(listOf(local), props).complete(request.copy(feature = "unknown"))

        assertEquals("local", result?.provider)
    }
}
