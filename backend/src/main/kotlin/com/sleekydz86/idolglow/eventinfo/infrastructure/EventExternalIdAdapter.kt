package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventExternalIdPort
import com.sleekydz86.idolglow.eventinfo.domain.EventExternalId
import com.sleekydz86.idolglow.eventinfo.domain.ExternalIdScheme
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface EventExternalIdJpaRepository : JpaRepository<EventExternalId, Long> {
    fun findBySchemeAndExternalId(
        scheme: ExternalIdScheme,
        externalId: String,
    ): EventExternalId?

    fun findByGlobalIdIn(globalIds: Collection<String>): List<EventExternalId>

    fun findByGlobalId(globalId: String): List<EventExternalId>
}

@Repository
class EventExternalIdAdapter(
    private val repository: EventExternalIdJpaRepository,
) : EventExternalIdPort {
    override fun save(externalId: EventExternalId): EventExternalId = repository.saveAndFlush(externalId)

    override fun find(
        scheme: ExternalIdScheme,
        externalId: String,
    ): EventExternalId? = repository.findBySchemeAndExternalId(scheme, externalId)

    override fun findByGlobalIdIn(globalIds: Collection<String>): List<EventExternalId> =
        if (globalIds.isEmpty()) emptyList() else repository.findByGlobalIdIn(globalIds)

    override fun findByGlobalId(globalId: String): List<EventExternalId> = repository.findByGlobalId(globalId)

    override fun delete(externalId: EventExternalId) = repository.delete(externalId)
}
