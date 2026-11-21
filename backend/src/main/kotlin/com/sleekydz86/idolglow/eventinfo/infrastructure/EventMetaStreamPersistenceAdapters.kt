package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.ConsumedEventPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventInterestPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMetaDeadLetterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMetaProjectionPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventOutboxPort
import com.sleekydz86.idolglow.eventinfo.domain.ConsumedEvent
import com.sleekydz86.idolglow.eventinfo.domain.EventInterest
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaDeadLetter
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaProjection
import com.sleekydz86.idolglow.eventinfo.domain.EventOutbox
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

interface EventOutboxJpaRepository : JpaRepository<EventOutbox, Long> {
    @Query("select o from EventOutbox o where o.sentAt is null order by o.id asc")
    fun findUnsent(pageable: Pageable): List<EventOutbox>

    @Modifying
    @Query("delete from EventOutbox o where o.sentAt is not null and o.sentAt < :threshold")
    fun deleteSentBefore(
        @Param("threshold") threshold: LocalDateTime,
    ): Int
}

interface ConsumedEventJpaRepository : JpaRepository<ConsumedEvent, Long> {
    fun existsByConsumerNameAndEventId(
        consumerName: String,
        eventId: String,
    ): Boolean
}

interface EventMetaProjectionJpaRepository : JpaRepository<EventMetaProjection, String>

interface EventInterestJpaRepository : JpaRepository<EventInterest, Long> {
    fun findByUserIdAndGlobalId(
        userId: Long,
        globalId: String,
    ): EventInterest?

    fun findByUserIdOrderByIdDesc(userId: Long): List<EventInterest>

    @Query("select i.userId from EventInterest i where i.globalId = :globalId")
    fun findUserIdsByGlobalId(
        @Param("globalId") globalId: String,
    ): List<Long>
}

interface EventMetaDeadLetterJpaRepository : JpaRepository<EventMetaDeadLetter, Long> {
    fun existsByConsumerNameAndEventId(
        consumerName: String,
        eventId: String,
    ): Boolean

    fun findTop100ByOrderByIdDesc(): List<EventMetaDeadLetter>

    fun findTop100ByReprocessedAtIsNullOrderByIdDesc(): List<EventMetaDeadLetter>
}

@Repository
class EventOutboxAdapter(
    private val repository: EventOutboxJpaRepository,
) : EventOutboxPort {
    override fun save(outbox: EventOutbox): EventOutbox = repository.save(outbox)

    override fun findUnsent(limit: Int): List<EventOutbox> = repository.findUnsent(PageRequest.of(0, limit))

    override fun findById(id: Long): EventOutbox? = repository.findById(id).orElse(null)

    @Transactional
    override fun deleteSentBefore(threshold: LocalDateTime): Int = repository.deleteSentBefore(threshold)
}

@Repository
class ConsumedEventAdapter(
    private val repository: ConsumedEventJpaRepository,
) : ConsumedEventPort {
    override fun exists(
        consumerName: String,
        eventId: String,
    ): Boolean = repository.existsByConsumerNameAndEventId(consumerName, eventId)

    override fun save(consumed: ConsumedEvent): ConsumedEvent = repository.save(consumed)
}

@Repository
class EventMetaProjectionAdapter(
    private val repository: EventMetaProjectionJpaRepository,
) : EventMetaProjectionPort {
    override fun findByGlobalId(globalId: String): EventMetaProjection? = repository.findById(globalId).orElse(null)

    override fun save(projection: EventMetaProjection): EventMetaProjection = repository.save(projection)
}

@Repository
class EventInterestAdapter(
    private val repository: EventInterestJpaRepository,
) : EventInterestPort {
    override fun save(interest: EventInterest): EventInterest = repository.save(interest)

    override fun find(
        userId: Long,
        globalId: String,
    ): EventInterest? = repository.findByUserIdAndGlobalId(userId, globalId)

    override fun delete(interest: EventInterest) = repository.delete(interest)

    override fun findByUserId(userId: Long): List<EventInterest> = repository.findByUserIdOrderByIdDesc(userId)

    override fun findUserIdsByGlobalId(globalId: String): List<Long> = repository.findUserIdsByGlobalId(globalId)
}

@Repository
class EventMetaDeadLetterAdapter(
    private val repository: EventMetaDeadLetterJpaRepository,
) : EventMetaDeadLetterPort {
    override fun save(deadLetter: EventMetaDeadLetter): EventMetaDeadLetter = repository.save(deadLetter)

    override fun findById(id: Long): EventMetaDeadLetter? = repository.findById(id).orElse(null)

    override fun exists(
        consumerName: String,
        eventId: String,
    ): Boolean = repository.existsByConsumerNameAndEventId(consumerName, eventId)

    override fun findRecent(pendingOnly: Boolean): List<EventMetaDeadLetter> =
        if (pendingOnly) {
            repository.findTop100ByReprocessedAtIsNullOrderByIdDesc()
        } else {
            repository.findTop100ByOrderByIdDesc()
        }
}
