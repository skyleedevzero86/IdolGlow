package com.sleekydz86.idolglow.servermetric.application

import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration

@DisplayName("HTTP 응답 수 차이")
class HttpResponseCounterTest {
    @DisplayName("첫 점검은 누적 값 그대로, 다음 점검은 직전과의 차이를 주고 500 이상은 따로 센다")
    @Test
    fun returnsDeltaSinceLastCheck() {
        // given
        val registry = SimpleMeterRegistry()
        val counter = HttpResponseCounter(registry)
        record(registry, "SUCCESS", 3)
        record(registry, "SERVER_ERROR", 1)

        // when
        val first = counter.takeSinceLast()
        record(registry, "SUCCESS", 2)
        record(registry, "CLIENT_ERROR", 1)
        record(registry, "SERVER_ERROR", 2)
        val second = counter.takeSinceLast()

        // then
        assertEquals(HttpResponseDelta(4, 1), first)
        assertEquals(HttpResponseDelta(5, 2), second)
    }

    @DisplayName("누적 값이 줄면 서버가 다시 켜진 것으로 보고 지금 누적 값을 그대로 쓴다")
    @Test
    fun usesCurrentWhenCumulativeDrops() {
        // given

        // when
        val dropped = HttpResponseCounter.deltaOf(current = 7, previous = 100)
        val normal = HttpResponseCounter.deltaOf(current = 107, previous = 100)
        val first = HttpResponseCounter.deltaOf(current = 7, previous = null)

        // then
        assertEquals(7L, dropped)
        assertEquals(7L, normal)
        assertEquals(7L, first)
    }

    private fun record(
        registry: SimpleMeterRegistry,
        outcome: String,
        times: Int,
    ) {
        val timer =
            Timer
                .builder(HttpResponseCounter.HTTP_SERVER_REQUESTS)
                .tag(HttpResponseCounter.OUTCOME_TAG, outcome)
                .tag("uri", "/visits")
                .register(registry)
        repeat(times) { timer.record(Duration.ofMillis(5)) }
    }
}
