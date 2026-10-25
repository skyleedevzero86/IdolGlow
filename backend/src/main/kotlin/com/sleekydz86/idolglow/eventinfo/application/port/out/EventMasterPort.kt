package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.eventinfo.domain.EventMaster

interface EventMasterPort {
    fun save(master: EventMaster): EventMaster

    fun findByGlobalId(globalId: String): EventMaster?

    fun findByNormalizedTitle(normalizedTitle: String): List<EventMaster>

    fun count(): Long
}
