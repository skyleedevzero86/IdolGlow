package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventOutboxPort
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Component
@ConditionalOnProperty(prefix = "app.event-meta.stream", name = ["enabled"], havingValue = "true")
class EventOutboxRelay(
    private val eventOutboxPort: EventOutboxPort,
    private val eventOutboxTxService: EventOutboxTxService,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val properties: EventMetaStreamProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${app.event-meta.stream.relay-interval-ms:1000}")
    @SchedulerLock(name = "eventOutboxRelay", lockAtMostFor = "PT1M")
    fun relayScheduled() {
        relayOnce()
    }

    fun relayOnce(): Int {
        var sent = 0
        for (row in eventOutboxPort.findUnsent(properties.relayBatchSize)) {
            val delivered =
                runCatching {
                    kafkaTemplate
                        .send(properties.topic, row.aggregateId, row.payload)
                        .get(properties.sendTimeoutSeconds, TimeUnit.SECONDS)
                }
            if (delivered.isFailure) {
                log.warn(
                    "공연 메타 이벤트 발행 실패, 다음 주기에 다시 보냅니다. outboxId={} globalId={} error={}",
                    row.id,
                    row.aggregateId,
                    delivered.exceptionOrNull()?.javaClass?.simpleName,
                )
                break
            }
            eventOutboxTxService.markSent(row.id)
            sent += 1
        }
        return sent
    }

    @Scheduled(cron = "\${app.event-meta.stream.cleanup-cron:0 30 4 * * *}", zone = "Asia/Seoul")
    @SchedulerLock(name = "eventOutboxCleanup", lockAtMostFor = "PT10M")
    fun cleanup() {
        val deleted = eventOutboxPort.deleteSentBefore(LocalDateTime.now().minusDays(properties.retentionDays))
        log.info("보낸 공연 메타 이벤트 정리. deleted={} retentionDays={}", deleted, properties.retentionDays)
    }
}
