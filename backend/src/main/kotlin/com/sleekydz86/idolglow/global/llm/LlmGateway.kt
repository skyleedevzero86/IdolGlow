package com.sleekydz86.idolglow.global.llm

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
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
    private val tracer: LlmCallTracer,
) {
    internal var clock: Clock = Clock.system(ZoneId.of("Asia/Seoul"))
    private val log = LoggerFactory.getLogger(javaClass)
    private val byName = providers.associateBy { it.name }
    private val externalCalls = AtomicInteger(0)
    private val counterDate = AtomicReference<LocalDate?>(null)

    fun isAvailable(feature: String): Boolean = usableProviders(feature).isNotEmpty()

    fun complete(request: LlmRequest): LlmResult? =
        usableProviders(request.feature)
            .firstNotNullOfOrNull { provider ->
                call(provider, request)?.also { if (provider.external) externalCalls.incrementAndGet() }
            }

    private fun usableProviders(feature: String): List<LlmProvider> {
        val chain = props.chains[feature] ?: props.chains[DEFAULT_CHAIN] ?: listOf("local")
        return chain
            .mapNotNull { byName[it] }
            .filter { it.isConfigured() && (!it.external || allowExternal()) }
    }

    private fun call(
        provider: LlmProvider,
        request: LlmRequest,
    ): LlmResult? {
        val safeRequest = if (provider.external) request.copy(userPrompt = masker.mask(request.userPrompt)) else request
        val breaker = circuitBreakers.circuitBreaker("llm-${provider.name}")
        val sample = Timer.start(meters)
        val startedAt = clock.instant()
        val outcome = runCatching { breaker.executeSupplier { provider.complete(safeRequest) } }
        val completion = outcome.getOrNull()
        val label = outcomeLabel(outcome)
        outcome.exceptionOrNull()?.takeUnless { it is CallNotPermittedException }?.let {
            log.warn("LLM 호출 실패 feature={} provider={} error={}", request.feature, provider.name, it.message)
        }
        val elapsedNs = record(sample, request.feature, provider.name, label)
        trace(provider.name, safeRequest, startedAt, label, outcome)
        return completion?.takeIf { label == SUCCESS }?.let {
            LlmResult(it.text, provider.name, TimeUnit.NANOSECONDS.toMillis(elapsedNs))
        }
    }

    private fun outcomeLabel(outcome: Result<LlmCompletion?>): String =
        when {
            outcome.exceptionOrNull() is CallNotPermittedException -> "circuit_open"
            outcome.isFailure -> "error"
            outcome.getOrNull()?.text.isNullOrBlank() -> "empty"
            else -> SUCCESS
        }

    private fun trace(
        providerName: String,
        request: LlmRequest,
        startedAt: Instant,
        label: String,
        outcome: Result<LlmCompletion?>,
    ) {
        if (!tracer.isEnabled()) return
        val trace =
            LlmCallTrace(
                feature = request.feature,
                provider = providerName,
                outcome = label,
                promptVersion = request.promptVersion,
                startedAt = startedAt,
                endedAt = clock.instant(),
                systemPrompt = request.systemPrompt,
                userPrompt = request.userPrompt,
                completion = outcome.getOrNull(),
                errorType = outcome.exceptionOrNull()?.javaClass?.simpleName,
            )
        runCatching { tracer.record(trace) }.onFailure {
            log.warn(
                "LLM 호출 추적 기록 실패 feature={} provider={} error={}",
                request.feature,
                providerName,
                it.javaClass.simpleName,
            )
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
        private const val SUCCESS = "success"
    }
}
