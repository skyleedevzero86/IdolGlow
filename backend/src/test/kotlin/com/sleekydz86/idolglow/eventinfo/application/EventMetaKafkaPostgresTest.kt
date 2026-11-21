package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaChangeType
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaMessage
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMasterJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMetaDeadLetterJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMetaProjectionJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventOutboxJpaRepository
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.notification.infrastructure.NotificationJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
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
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@DisplayName("공연 메타 이벤트 Kafka 공급 (Postgres + Kafka)")
class EventMetaKafkaPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var eventOutboxRelay: EventOutboxRelay

    @Autowired private lateinit var eventMetaOutboxRecorder: EventMetaOutboxRecorder

    @Autowired private lateinit var eventMasterJpaRepository: EventMasterJpaRepository

    @Autowired private lateinit var eventOutboxJpaRepository: EventOutboxJpaRepository

    @Autowired private lateinit var eventMetaProjectionJpaRepository: EventMetaProjectionJpaRepository

    @Autowired private lateinit var eventInterestService: EventInterestService

    @Autowired private lateinit var notificationJpaRepository: NotificationJpaRepository

    @Autowired private lateinit var userJpaRepository: UserJpaRepository

    @Autowired private lateinit var eventMetaDeadLetterService: EventMetaDeadLetterService

    @Autowired private lateinit var eventMetaDeadLetterJpaRepository: EventMetaDeadLetterJpaRepository

    @Autowired private lateinit var objectMapper: ObjectMapper

    @Autowired private lateinit var transactionTemplate: TransactionTemplate

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @AfterEach
    fun clean() {
        재개()
        jdbcTemplate.update("delete from consumed_event")
        jdbcTemplate.update("delete from event_meta_projection")
        jdbcTemplate.update("delete from event_outbox")
        jdbcTemplate.update("delete from event_interest")
        jdbcTemplate.update("delete from event_meta_dead_letter")
        jdbcTemplate.execute("drop table if exists shedlock")
        jdbcTemplate.update("delete from notifications where type = 'EVENT_INTEREST_CHANGED'")
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

    @DisplayName("T5 중계가 보낸 뒤 sent_at을 기록하기 전에 멈춰 같은 이벤트가 다시 보내져도, 소비자 결과는 한 번 처리한 것과 같다")
    @Test
    fun resendAfterCrashIsProcessedOnce() {
        // given
        val user = userJpaRepository.save(User.of(email = "t5${UUID.randomUUID().toString().take(8)}@idolglow.test"))
        val globalId = 공연_변경()
        eventInterestService.register(user.id, globalId)
        val scheduleEventId = 변경_기록(globalId, setOf("startDate"))
        eventOutboxRelay.relayOnce()
        처리될_때까지(scheduleEventId)
        jdbcTemplate.update("update event_outbox set sent_at = null where event_id = ?", scheduleEventId)

        // when
        val resent = eventOutboxRelay.relayOnce()
        val markerEventId = 변경_기록(globalId, setOf("translation"))
        eventOutboxRelay.relayOnce()
        처리될_때까지(markerEventId)

        // then
        assertEquals(1, resent)
        assertEquals(2, 처리_건수(scheduleEventId))
        assertEquals(3L, eventMetaProjectionJpaRepository.findById(globalId).get().metaVersion)
        val alerts = notificationJpaRepository.findAllByUserIdOrderByCreatedAtDesc(user.id)
        assertEquals(listOf(NotificationType.EVENT_INTEREST_CHANGED), alerts.map { it.type })
    }

    @DisplayName("T6 읽기 모델 소비자가 3번 연속 실패하면 실패 토픽을 거쳐 실패 목록에 남고, 원인을 없앤 뒤 관리 기능으로 다시 처리하면 반영된다")
    @Test
    fun deadLetterIsReprocessedByAdmin() {
        // given
        val globalId = 공연_변경()
        val createdEventId = jdbcTemplate.queryForObject(
            "select event_id from event_outbox where aggregate_id = ?",
            String::class.java,
            globalId,
        )!!
        eventOutboxRelay.relayOnce()
        처리될_때까지(createdEventId)

        // when
        val updatedEventId = 변경_기록(globalId, setOf("venue"))
        val deadLetter =
            읽기_모델을_잠근_채로(globalId) {
                eventOutboxRelay.relayOnce()
                실패_목록에_남을_때까지(updatedEventId)
            }
        val reprocessed = eventMetaDeadLetterService.reprocess(deadLetter.id)
        val reprocessedAgain = runCatching { eventMetaDeadLetterService.reprocess(deadLetter.id) }

        // then
        assertEquals(EventMetaProjectionService.CONSUMER, deadLetter.consumerName)
        assertEquals("org.springframework.dao.CannotAcquireLockException", deadLetter.errorType)
        assertEquals(EventMetaApplyResult.APPLIED, reprocessed.result)
        assertEquals(2L, eventMetaProjectionJpaRepository.findById(globalId).get().metaVersion)
        assertTrue(eventMetaDeadLetterJpaRepository.findById(deadLetter.id).get().isReprocessed())
        assertTrue(reprocessedAgain.isFailure)
        assertEquals(emptyList<EventMetaDeadLetterView>(), eventMetaDeadLetterService.list(pendingOnly = true))
    }

    @DisplayName("T7 다른 인스턴스가 중계 잠금을 잡고 있으면 이번 중계는 건너뛰고, 잠금이 풀리면 보낸다")
    @Test
    fun skipsWhileAnotherInstanceHoldsLock() {
        // given
        jdbcTemplate.execute(SHEDLOCK_DDL)
        val globalIds = (1..EVENT_COUNT).map { 공연_변경() }
        jdbcTemplate.update(
            "insert into shedlock (name, lock_until, locked_at, locked_by) " +
                "values (?, timezone('utc', current_timestamp) + interval '1 minute', " +
                "timezone('utc', current_timestamp), ?)",
            RELAY_LOCK,
            "other-instance",
        )

        // when
        eventOutboxRelay.relayScheduled()
        val unsentWhileLocked = 안_보낸_건수(globalIds)
        jdbcTemplate.update(
            "update shedlock set lock_until = timezone('utc', current_timestamp) where name = ?",
            RELAY_LOCK,
        )
        eventOutboxRelay.relayScheduled()

        // then
        assertEquals(EVENT_COUNT, unsentWhileLocked)
        assertEquals(0, 안_보낸_건수(globalIds))
        assertEquals(EVENT_COUNT, 발행된_이벤트_ID(globalIds.toSet()).size)
    }

    @DisplayName("T7 중계 두 개가 동시에 돌아도 하나만 실행돼 이벤트마다 Kafka에 한 번씩만 발행된다")
    @Test
    fun concurrentRelaysPublishEachEventOnce() {
        // given
        jdbcTemplate.execute(SHEDLOCK_DDL)
        val globalIds = (1..CONCURRENT_EVENT_COUNT).map { 공연_변경() }.toSet()
        val barrier = CyclicBarrier(2)

        // when
        val relays =
            (1..2).map {
                thread {
                    barrier.await()
                    eventOutboxRelay.relayScheduled()
                }
            }
        relays.forEach { it.join() }
        val published = 발행된_이벤트_ID(globalIds)

        // then
        assertEquals(0, 안_보낸_건수(globalIds.toList()))
        assertEquals(CONCURRENT_EVENT_COUNT, published.size)
        assertEquals(published.size, published.toSet().size)
    }

    private fun 발행된_이벤트_ID(globalIds: Set<String>): List<String> =
        KafkaConsumer<String, String>(
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ),
        ).use { consumer ->
            val partitions = consumer.partitionsFor(TOPIC).map { TopicPartition(it.topic(), it.partition()) }
            consumer.assign(partitions)
            consumer.seekToBeginning(partitions)
            val end = consumer.endOffsets(partitions)
            val eventIds = mutableListOf<String>()
            while (partitions.any { consumer.position(it) < end.getValue(it) }) {
                consumer
                    .poll(Duration.ofMillis(POLL_MILLIS))
                    .filter { it.key() in globalIds }
                    .forEach { eventIds += objectMapper.readValue(it.value(), EventMetaMessage::class.java).eventId }
            }
            eventIds
        }

    private fun 읽기_모델을_잠근_채로(
        globalId: String,
        block: () -> EventMetaDeadLetterView,
    ): EventMetaDeadLetterView {
        val locked = CountDownLatch(1)
        val release = CountDownLatch(1)
        val holder =
            thread {
                transactionTemplate.executeWithoutResult {
                    jdbcTemplate.queryForList(
                        "select global_id from event_meta_projection where global_id = ? for update",
                        globalId,
                    )
                    locked.countDown()
                    release.await(WAIT.seconds, TimeUnit.SECONDS)
                }
            }
        locked.await(WAIT.seconds, TimeUnit.SECONDS)
        try {
            return block()
        } finally {
            release.countDown()
            holder.join()
        }
    }

    private fun 실패_목록에_남을_때까지(eventId: String): EventMetaDeadLetterView {
        val deadline = Instant.now().plus(WAIT)
        var found = 실패_목록(eventId)
        while (found == null && Instant.now().isBefore(deadline)) {
            Thread.sleep(POLL_MILLIS)
            found = 실패_목록(eventId)
        }
        return requireNotNull(found) { "실패 목록에 남지 않았습니다. eventId=$eventId" }
    }

    private fun 실패_목록(eventId: String): EventMetaDeadLetterView? =
        eventMetaDeadLetterService.list(pendingOnly = true).firstOrNull { it.eventId == eventId }

    private fun 변경_기록(
        globalId: String,
        changedFields: Set<String>,
    ): String =
        transactionTemplate.execute {
            val master = requireNotNull(eventMasterJpaRepository.findByGlobalId(globalId))
            eventMetaOutboxRecorder.record(master, EventMetaChangeType.UPDATED, changedFields).eventId
        }!!

    private fun 처리_건수(eventId: String): Int =
        jdbcTemplate.queryForObject(
            "select count(*) from consumed_event where event_id = ?",
            Int::class.java,
            eventId,
        )!!

    private fun 처리될_때까지(eventId: String) {
        val deadline = Instant.now().plus(WAIT)
        while (처리_건수(eventId) < CONSUMER_COUNT && Instant.now().isBefore(deadline)) {
            Thread.sleep(POLL_MILLIS)
        }
        assertEquals(CONSUMER_COUNT, 처리_건수(eventId))
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
        private const val CONSUMER_COUNT: Int = 2
        private const val CONCURRENT_EVENT_COUNT: Int = 30
        private const val TOPIC: String = "event-meta.changed.test"
        private const val RELAY_LOCK: String = "eventOutboxRelay"
        private const val SHEDLOCK_DDL: String =
            "create table if not exists shedlock (name varchar(64) not null primary key, " +
                "lock_until timestamp(6) not null, locked_at timestamp(6) not null, locked_by varchar(255) not null)"
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
            registry.add("app.event-meta.stream.topic") { TOPIC }
            registry.add("app.event-meta.stream.send-timeout-seconds") { "4" }
            registry.add("app.event-meta.stream.dead-letter-topic") { "event-meta.changed.test-dlt" }
            registry.add("app.event-meta.stream.retry-interval-ms") { "500" }
            registry.add("app.event-meta.stream.relay-interval-ms") { "3600000" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create" }
        }
    }
}
