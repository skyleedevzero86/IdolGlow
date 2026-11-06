package com.sleekydz86.idolglow.global.llm

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class LlmCircuitBreakerConfig {
    @Bean
    fun llmCircuitBreakerRegistry(): CircuitBreakerRegistry =
        CircuitBreakerRegistry.of(
            CircuitBreakerConfig
                .custom()
                .slidingWindowSize(SLIDING_WINDOW_SIZE)
                .minimumNumberOfCalls(MINIMUM_CALLS)
                .failureRateThreshold(FAILURE_RATE_PERCENT)
                .waitDurationInOpenState(Duration.ofSeconds(OPEN_STATE_SECONDS))
                .build(),
        )

    companion object {
        private const val SLIDING_WINDOW_SIZE = 10
        private const val MINIMUM_CALLS = 5
        private const val FAILURE_RATE_PERCENT = 50f
        private const val OPEN_STATE_SECONDS = 60L
    }
}
