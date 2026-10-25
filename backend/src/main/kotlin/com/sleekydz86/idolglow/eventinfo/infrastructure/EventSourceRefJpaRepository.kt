package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import org.springframework.data.jpa.repository.JpaRepository

interface EventSourceRefJpaRepository : JpaRepository<EventSourceRef, Long> {
    fun findBySourceAndSourceId(
        source: String,
        sourceId: String,
    ): EventSourceRef?

    fun findBySourceInAndSourceIdIn(
        sources: Collection<String>,
        sourceIds: Collection<String>,
    ): List<EventSourceRef>

    fun countByGlobalId(globalId: String): Long

    fun countByMatchStatus(matchStatus: EventMatchStatus): Long

    fun findByMatchStatusIn(statuses: Collection<EventMatchStatus>): List<EventSourceRef>
}
