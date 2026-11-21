package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaChangeType
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMasterJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMetaProjectionJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventOutboxJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.DockerClientFactory
import org.testcontainers.kafka.KafkaContainer
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@DisplayName("공연 메타 이벤트 Kafka 공급 (Postgres + Kafka)")
class EventMetaKafkaPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var eventOutboxRelay: EventOutboxRelay

    @Autowired private lateinit var eventMetaOutboxRecorder: EventMetaOutboxRecorder

    @Autowired private lateinit var eventMasterJpaRepository: EventMasterJpaRepository

    @Autowired private lateinit var eventOutboxJpaRepository: EventOutboxJpaRepository

    @Autowired private lateinit var eventMetaProjectionJpaRepository: EventMetaProjectionJpaRepository

    @Autowired private lateinit var transactionTemplate: TransactionTemplate

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @AfterEach
    fun clean() {
        재개()
        jdbcTemplate.update("delete from consumed_event")
        jdbcTemplate.update("delete from event_meta_projection")
        jdbcTemplate.update("delete from event_outbox")
    }

    @DisplayName("T4 Kafka가 내려간 동안 바뀐 공연 N건은 outbox에 남았다가, 다시 올라오면 모두 반영되고 유실이 없다")
    @Test
    fun deliversEverythingAfterKafkaComesBack() {
        // given
        val globalIds = (1..EVENT_COUNT).map { 공연_변경() }
        멈춤()

        // when
        val sentWhileDown = eventOutboxRelay.relayOnce()
        val unsentWhileDown = 안_보낸_건수(globalIds)
        재개()
        val sentAfter = 다_보낼_때까지()

        // then
        assertEquals(0, sentWhileDown)
        assertEquals(EVENT_COUNT, unsentWhileDown)
        assertEquals(EVENT_COUNT, sentAfter)
        val projected = 반영될_때까지(globalIds)
        assertEquals(globalIds.toSet(), projected.map { it.globalId }.toSet())
        assertTrue(projected.all { it.metaVersion == 1L })
    }

    private fun 공연_변경(): String {
        val globalId = UUID.randomUUID().toString()
        transactionTemplate.executeWithoutResult {
            val master =
                eventMasterJpaRepository.save(
                    EventMaster(
                        globalId = globalId,
                        title = "공연 ${globalId.take(8)}",
                        normalizedTitle = "공연${globalId.take(8)}",
                        startDate = LocalDate.of(2026, 12, 1),
                        endDate = LocalDate.of(2026, 12, 3),
                        venue = "세종문화회관",
                        normalizedVenue = "세종문화회관",
                        status = EventMasterStatus.ACTIVE,
                    ),
                )
            eventMetaOutboxRecorder.record(master, EventMetaChangeType.CREATED, emptySet())
        }
        return globalId
    }

    private fun 안_보낸_건수(globalIds: List<String>): Int =
        eventOutboxJpaRepository.findAll().count { it.aggregateId in globalIds && it.sentAt == null }

    private fun 다_보낼_때까지(): Int {
        val deadline = Instant.now().plus(WAIT)
        var sent = 0
        while (sent < EVENT_COUNT && Instant.now().isBefore(deadline)) {
            sent += eventOutboxRelay.relayOnce()
            if (sent < EVENT_COUNT) Thread.sleep(POLL_MILLIS)
        }
        return sent
    }

    private fun 반영될_때까지(globalIds: List<String>) =
        run {
            val deadline = Instant.now().plus(WAIT)
            var rows = eventMetaProjectionJpaRepository.findAllById(globalIds)
            while (rows.size < globalIds.size && Instant.now().isBefore(deadline)) {
                Thread.sleep(POLL_MILLIS)
                rows = eventMetaProjectionJpaRepository.findAllById(globalIds)
            }
            rows
        }

    private fun 멈춤() {
        DockerClientFactory.instance().client().pauseContainerCmd(kafka.containerId).exec()
        paused = true
    }

    private fun 재개() {
        if (!paused) return
        DockerClientFactory.instance().client().unpauseContainerCmd(kafka.containerId).exec()
        paused = false
    }

    companion object {
        private const val EVENT_COUNT: Int = 5
        private const val POLL_MILLIS: Long = 500
        private val WAIT: Duration = Duration.ofSeconds(60)
        private var paused: Boolean = false

        @JvmStatic
        val kafka: KafkaContainer = KafkaContainer("apache/kafka:3.9.1").apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun kafkaProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
            registry.add("spring.kafka.consumer.auto-offset-reset") { "earliest" }
            registry.add("spring.kafka.producer.properties.max.block.ms") { "3000" }
            registry.add("spring.kafka.producer.properties.request.timeout.ms") { "2000" }
            registry.add("spring.kafka.producer.properties.delivery.timeout.ms") { "3000" }
            registry.add("app.event-meta.stream.enabled") { "true" }
            registry.add("app.event-meta.stream.topic") { "event-meta.changed.test" }
            registry.add("app.event-meta.stream.send-timeout-seconds") { "4" }
        }
    }
}
