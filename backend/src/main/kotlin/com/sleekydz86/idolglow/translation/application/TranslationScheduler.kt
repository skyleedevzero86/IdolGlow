package com.sleekydz86.idolglow.translation.application

import com.sleekydz86.idolglow.translation.application.port.out.ContentTranslationPort
import com.sleekydz86.idolglow.translation.domain.TranslationStatus
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class TranslationScheduler(
    private val scanService: TranslationScanService,
    private val workerService: TranslationWorkerService,
    private val contentTranslationPort: ContentTranslationPort,
    private val props: TranslationProperties,
    meterRegistry: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(TranslationScheduler::class.java)
    private val rows: Map<TranslationStatus, AtomicLong> = TranslationStatus.entries.associateWith { AtomicLong() }

    init {
        rows.forEach { (status, value) ->
            Gauge
                .builder(ROWS_METRIC, value) { it.get().toDouble() }
                .tag("status", status.name.lowercase())
                .strongReference(true)
                .register(meterRegistry)
        }
    }

    @Scheduled(
        initialDelayString = "\${app.translation.scan-initial-delay-ms:120000}",
        fixedDelayString = "\${app.translation.scan-interval-ms:1800000}",
    )
    @SchedulerLock(name = "translationScan", lockAtLeastFor = "PT30S", lockAtMostFor = "PT20M")
    fun scan() {
        if (!props.enabled) return
        val queued = scanService.scan()
        if (queued.values.sum() > 0) log.info("번역 대상 스캔 결과. queued={}", queued)
    }

    @Scheduled(
        initialDelayString = "\${app.translation.work-initial-delay-ms:180000}",
        fixedDelayString = "\${app.translation.work-interval-ms:60000}",
    )
    @SchedulerLock(name = "translationWork", lockAtLeastFor = "PT10S", lockAtMostFor = "PT15M")
    fun work() {
        val summary = workerService.runOnce()
        if (summary.processed > 0) log.info("번역 작업 결과. outcomes={}", summary.outcomes)
    }

    @Scheduled(
        initialDelayString = "\${app.translation.metrics-initial-delay-ms:60000}",
        fixedDelayString = "\${app.translation.metrics-interval-ms:300000}",
    )
    fun refreshMetrics() {
        runCatching {
            rows.forEach { (status, value) -> value.set(contentTranslationPort.countByStatus(status, null)) }
        }.onFailure { log.warn("번역 지표 갱신에 실패했습니다.", it) }
    }

    fun rows(status: TranslationStatus): Long? = rows[status]?.get()

    companion object {
        const val ROWS_METRIC: String = "idolglow.translation.rows"
    }
}
