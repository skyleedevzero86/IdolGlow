package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.eventinfo.domain.EventExternalId
import com.sleekydz86.idolglow.eventinfo.domain.ExternalIdScheme

interface EventExternalIdPort {
    fun save(externalId: EventExternalId): EventExternalId

    fun find(
        scheme: ExternalIdScheme,
        externalId: String,
    ): EventExternalId?

    fun findByGlobalIdIn(globalIds: Collection<String>): List<EventExternalId>

    fun findByGlobalId(globalId: String): List<EventExternalId>

    fun delete(externalId: EventExternalId)
}
