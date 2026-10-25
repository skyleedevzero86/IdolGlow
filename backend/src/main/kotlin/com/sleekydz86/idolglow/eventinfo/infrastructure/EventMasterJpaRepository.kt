package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import org.springframework.data.jpa.repository.JpaRepository

interface EventMasterJpaRepository : JpaRepository<EventMaster, Long> {
    fun findByGlobalId(globalId: String): EventMaster?

    fun findByNormalizedTitle(normalizedTitle: String): List<EventMaster>
}
