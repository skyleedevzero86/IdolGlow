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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.Executor

@DisplayName("설문 추천 LLM 비동기 저장소")
class SurveyLlmRecommendationStoreTest {
    private class FakeProvider(
        private val answers: ArrayDeque<String?>,
    ) : LlmProvider {
        override val name = "local"
        override val external = false
        var calls = 0

        override fun isConfigured(): Boolean = true

        override fun complete(request: LlmRequest): String? {
            calls++
            return answers.removeFirstOrNull()
        }
    }

    private class ManualExecutor : Executor {
        val tasks = ArrayDeque<Runnable>()

        override fun execute(command: Runnable) {
            tasks += command
        }

        fun runAll() {
            while (tasks.isNotEmpty()) tasks.removeFirst().run()
        }
    }

    private val objectMapper = ObjectMapper()
    private val attractions =
        listOf(TourAttraction("A1", "경복궁", 1, "서울", 11530, "종로구", "관광지", "역사관광지", 1, null, null, "202610"))
    private val good =
        objectMapper.writeValueAsString(
            mapOf(
                "title" to "서울 전통 코스",
                "subtitle" to "전통 문화",
                "narrative" to "경복궁을 둘러보세요.",
                "attractionReasons" to mapOf("A1" to "전통 문화를 느낄 수 있습니다."),
            ),
        )

    private fun store(
        provider: FakeProvider,
        executor: Executor,
    ): SurveyLlmRecommendationStore {
        val props = LlmGatewayProperties(chains = mapOf("survey" to listOf("local")))
        val gateway =
            LlmGateway(
                listOf(provider),
                props,
                CircuitBreakerRegistry.ofDefaults(),
                SimpleMeterRegistry(),
                LlmInputMasker(),
            )
        return SurveyLlmRecommendationStore(SurveyRecommendationLlmRouter(gateway, objectMapper), executor)
    }

    @DisplayName("첫 조회는 바로 null과 대기 상태를 돌려주고, 작업이 끝나면 다음 조회에서 결과를 준다")
    @Test
    fun returnsImmediatelyThenServesResult() {
        val provider = FakeProvider(ArrayDeque(listOf(good)))
        val executor = ManualExecutor()
        val store = store(provider, executor)

        assertNull(store.findOrRequest(1L, listOf("BTS"), attractions))
        assertTrue(store.isPending(1L))
        assertEquals(0, provider.calls)

        executor.runAll()

        assertNotNull(store.findOrRequest(1L, listOf("BTS"), attractions))
        assertFalse(store.isPending(1L))
        assertEquals(1, provider.calls)
    }

    @DisplayName("대기 중에 다시 조회해도 작업을 한 번만 넣는다")
    @Test
    fun deduplicatesWhilePending() {
        val executor = ManualExecutor()
        val store = store(FakeProvider(ArrayDeque(listOf(good))), executor)

        repeat(3) { store.findOrRequest(2L, listOf("BTS"), attractions) }

        assertEquals(1, executor.tasks.size)
    }

    @DisplayName("실패하면 10분 동안 다시 시도하지 않는다")
    @Test
    fun waitsBeforeRetryAfterFailure() {
        val executor = ManualExecutor()
        val provider = FakeProvider(ArrayDeque(listOf(null, good)))
        val store = store(provider, executor)
        val start = Instant.parse("2026-11-07T00:00:00Z")
        store.clock = Clock.fixed(start, ZoneOffset.UTC)

        store.findOrRequest(3L, listOf("BTS"), attractions)
        executor.runAll()
        store.findOrRequest(3L, listOf("BTS"), attractions)
        assertTrue(executor.tasks.isEmpty())

        store.clock = Clock.fixed(start.plusSeconds(601), ZoneOffset.UTC)
        store.findOrRequest(3L, listOf("BTS"), attractions)
        executor.runAll()

        assertNotNull(store.findOrRequest(3L, listOf("BTS"), attractions))
        assertEquals(2, provider.calls)
    }

    @DisplayName("큐가 가득 차 거절되면 대기 상태를 풀어 다음 조회에서 다시 넣는다")
    @Test
    fun releasesPendingWhenRejected() {
        val store = store(FakeProvider(ArrayDeque(listOf(good))), Executor { error("queue full") })

        assertNull(store.findOrRequest(4L, listOf("BTS"), attractions))
        assertFalse(store.isPending(4L))
    }
}
