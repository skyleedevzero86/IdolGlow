package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import org.springframework.stereotype.Repository

@Repository
class EventSourceRefRepositoryImpl(
    private val eventSourceRefJpaRepository: EventSourceRefJpaRepository,
) : EventSourceRefPort {
    override fun save(ref: EventSourceRef): EventSourceRef = eventSourceRefJpaRepository.saveAndFlush(ref)

    override fun findBySourceAndSourceId(
        source: String,
        sourceId: String,
    ): EventSourceRef? = eventSourceRefJpaRepository.findBySourceAndSourceId(source, sourceId)

    override fun countByGlobalId(globalId: String): Long = eventSourceRefJpaRepository.countByGlobalId(globalId)

    override fun countByMatchStatus(status: EventMatchStatus): Long =
        eventSourceRefJpaRepository.countByMatchStatus(status)

    override fun findAll(): List<EventSourceRef> = eventSourceRefJpaRepository.findAll()
}
