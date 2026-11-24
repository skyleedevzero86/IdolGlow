package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class EventMasterRepositoryImpl(
    private val eventMasterJpaRepository: EventMasterJpaRepository,
) : EventMasterPort {
    override fun save(master: EventMaster): EventMaster = eventMasterJpaRepository.saveAndFlush(master)

    override fun findByGlobalId(globalId: String): EventMaster? = eventMasterJpaRepository.findByGlobalId(globalId)

    override fun findActiveByNormalizedTitle(normalizedTitle: String): List<EventMaster> =
        eventMasterJpaRepository.findByNormalizedTitleAndStatus(normalizedTitle, EventMasterStatus.ACTIVE)

    override fun findChangedAfter(
        updatedAt: LocalDateTime,
        globalId: String,
        until: LocalDateTime,
        limit: Int,
    ): List<EventMaster> = eventMasterJpaRepository.findChangedAfter(updatedAt, globalId, until, PageRequest.of(0, limit))

    override fun count(): Long = eventMasterJpaRepository.count()
}
