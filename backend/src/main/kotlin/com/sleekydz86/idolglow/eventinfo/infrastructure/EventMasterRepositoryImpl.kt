package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import org.springframework.stereotype.Repository

@Repository
class EventMasterRepositoryImpl(
    private val eventMasterJpaRepository: EventMasterJpaRepository,
) : EventMasterPort {
    override fun save(master: EventMaster): EventMaster = eventMasterJpaRepository.saveAndFlush(master)

    override fun findByGlobalId(globalId: String): EventMaster? = eventMasterJpaRepository.findByGlobalId(globalId)

    override fun findByNormalizedTitle(normalizedTitle: String): List<EventMaster> =
        eventMasterJpaRepository.findByNormalizedTitle(normalizedTitle)

    override fun count(): Long = eventMasterJpaRepository.count()
}
