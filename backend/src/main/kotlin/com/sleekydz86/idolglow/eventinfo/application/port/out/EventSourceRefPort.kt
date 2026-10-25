package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef

interface EventSourceRefPort {
    fun save(ref: EventSourceRef): EventSourceRef

    fun findBySourceAndSourceId(
        source: String,
        sourceId: String,
    ): EventSourceRef?

    fun findBySourceInAndSourceIdIn(
        sources: Collection<String>,
        sourceIds: Collection<String>,
    ): List<EventSourceRef>

    fun countByGlobalId(globalId: String): Long

    fun countByMatchStatus(status: EventMatchStatus): Long

    fun findByMatchStatusIn(statuses: Collection<EventMatchStatus>): List<EventSourceRef>

    fun findAll(): List<EventSourceRef>
}
