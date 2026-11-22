package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationLocales
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class EventTranslationCoverageMetrics(
    private val coverageService: EventTranslationCoverageService,
    meterRegistry: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(EventTranslationCoverageMetrics::class.java)
    private val missing: Map<String, AtomicLong> = EventTranslationLocales.TARGETS.associateWith { AtomicLong() }
    private val machineOnly: Map<String, AtomicLong> = EventTranslationLocales.TARGETS.associateWith { AtomicLong() }

    init {
        EventTranslationLocales.TARGETS.forEach { locale ->
            register(meterRegistry, MISSING_METRIC, locale, missing.getValue(locale))
            register(meterRegistry, MACHINE_ONLY_METRIC, locale, machineOnly.getValue(locale))
        }
    }

    @Scheduled(
        initialDelayString = "\${eventinfo.translation-coverage.initial-delay-ms:60000}",
        fixedDelayString = "\${eventinfo.translation-coverage.refresh-ms:600000}",
    )
    fun refresh() {
        val result = runCatching { coverageService.coverage(sampleLimit = 0) }
        result.exceptionOrNull()?.let { error ->
            log.warn("번역 현황 지표 갱신에 실패했습니다.", error)
            return
        }
        result.getOrThrow().forEach { coverage ->
            missing[coverage.locale]?.set(coverage.missing)
            machineOnly[coverage.locale]?.set(coverage.machineOnly)
        }
    }

    fun missing(locale: String): Long? = missing[locale]?.get()

    fun machineOnly(locale: String): Long? = machineOnly[locale]?.get()

    private fun register(
        meterRegistry: MeterRegistry,
        name: String,
        locale: String,
        value: AtomicLong,
    ) {
        Gauge
            .builder(name, value) { it.get().toDouble() }
            .tag("locale", locale)
            .strongReference(true)
            .register(meterRegistry)
    }

    companion object {
        const val MISSING_METRIC: String = "idolglow.event.translation.missing"
        const val MACHINE_ONLY_METRIC: String = "idolglow.event.translation.machine_only"
    }
}
