package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import java.time.LocalDateTime

interface EventMasterPort {
    fun save(master: EventMaster): EventMaster

    fun findByGlobalId(globalId: String): EventMaster?

    fun findActiveByNormalizedTitle(normalizedTitle: String): List<EventMaster>

    fun findChangedAfter(
        updatedAt: LocalDateTime,
        globalId: String,
        until: LocalDateTime,
        limit: Int,
    ): List<EventMaster>

    fun count(): Long
}
