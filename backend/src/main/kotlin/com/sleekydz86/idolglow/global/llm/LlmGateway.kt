package com.sleekydz86.idolglow.global.llm

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Component
class LlmGateway(
    providers: List<LlmProvider>,
    private val props: LlmGatewayProperties,
    private val circuitBreakers: CircuitBreakerRegistry,
    private val meters: MeterRegistry,
    private val masker: LlmInputMasker,
) {
    internal var clock: Clock = Clock.system(ZoneId.of("Asia/Seoul"))
    private val log = LoggerFactory.getLogger(javaClass)
    private val byName = providers.associateBy { it.name }
    private val externalCalls = AtomicInteger(0)
    private val counterDate = AtomicReference<LocalDate?>(null)

    fun complete(request: LlmRequest): LlmResult? {
        val chain = props.chains[request.feature] ?: props.chains[DEFAULT_CHAIN] ?: listOf("local")
        return chain
            .mapNotNull { byName[it] }
            .filter { it.isConfigured() && (!it.external || allowExternal()) }
            .firstNotNullOfOrNull { provider ->
                call(provider, request)?.also { if (provider.external) externalCalls.incrementAndGet() }
            }
    }

    private fun call(
        provider: LlmProvider,
        request: LlmRequest,
    ): LlmResult? {
        val safeRequest = if (provider.external) request.copy(userPrompt = masker.mask(request.userPrompt)) else request
        val breaker = circuitBreakers.circuitBreaker("llm-${provider.name}")
        val sample = Timer.start(meters)
        val outcome = runCatching { breaker.executeSupplier { provider.complete(safeRequest) } }
        val text = outcome.getOrNull()
        val label =
            when {
                outcome.exceptionOrNull() is CallNotPermittedException -> "circuit_open"
                outcome.isFailure -> "error"
                text.isNullOrBlank() -> "empty"
                else -> "success"
            }
        outcome.exceptionOrNull()?.takeUnless { it is CallNotPermittedException }?.let {
            log.warn("LLM 호출 실패 feature={} provider={} error={}", request.feature, provider.name, it.message)
        }
        val elapsedNs = record(sample, request.feature, provider.name, label)
        return text?.takeIf { label == "success" }?.let {
            LlmResult(it, provider.name, TimeUnit.NANOSECONDS.toMillis(elapsedNs))
        }
    }

    private fun allowExternal(): Boolean {
        if (!props.externalEnabled) return false
        val today = LocalDate.now(clock)
        if (counterDate.get() != today) {
            counterDate.set(today)
            externalCalls.set(0)
        }
        return externalCalls.get() < props.externalDailyLimit
    }

    private fun record(
        sample: Timer.Sample,
        feature: String,
        provider: String,
        outcome: String,
    ): Long =
        sample.stop(
            Timer
                .builder("llm.call")
                .tag("feature", feature)
                .tag("provider", provider)
                .tag("outcome", outcome)
                .register(meters),
        )

    companion object {
        private const val DEFAULT_CHAIN = "default"
    }
}
