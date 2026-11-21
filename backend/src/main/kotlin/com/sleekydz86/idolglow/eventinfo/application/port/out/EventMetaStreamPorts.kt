package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.eventinfo.domain.ConsumedEvent
import com.sleekydz86.idolglow.eventinfo.domain.EventInterest
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaDeadLetter
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaProjection
import com.sleekydz86.idolglow.eventinfo.domain.EventOutbox
import java.time.LocalDateTime

interface EventOutboxPort {
    fun save(outbox: EventOutbox): EventOutbox

    fun findUnsent(limit: Int): List<EventOutbox>

    fun findById(id: Long): EventOutbox?

    fun deleteSentBefore(threshold: LocalDateTime): Int
}

interface ConsumedEventPort {
    fun exists(
        consumerName: String,
        eventId: String,
    ): Boolean

    fun save(consumed: ConsumedEvent): ConsumedEvent
}

interface EventMetaProjectionPort {
    fun findByGlobalId(globalId: String): EventMetaProjection?

    fun save(projection: EventMetaProjection): EventMetaProjection
}

interface EventInterestPort {
    fun save(interest: EventInterest): EventInterest

    fun find(
        userId: Long,
        globalId: String,
    ): EventInterest?

    fun delete(interest: EventInterest)

    fun findByUserId(userId: Long): List<EventInterest>

    fun findUserIdsByGlobalId(globalId: String): List<Long>
}

interface EventMetaDeadLetterPort {
    fun save(deadLetter: EventMetaDeadLetter): EventMetaDeadLetter

    fun findById(id: Long): EventMetaDeadLetter?

    fun exists(
        consumerName: String,
        eventId: String,
    ): Boolean

    fun findRecent(pendingOnly: Boolean): List<EventMetaDeadLetter>
}
