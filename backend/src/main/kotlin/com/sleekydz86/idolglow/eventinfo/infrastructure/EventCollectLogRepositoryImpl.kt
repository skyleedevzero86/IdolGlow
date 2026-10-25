package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventCollectLogPort
import com.sleekydz86.idolglow.eventinfo.domain.EventCollectLog
import org.springframework.stereotype.Repository

@Repository
class EventCollectLogRepositoryImpl(
    private val eventCollectLogJpaRepository: EventCollectLogJpaRepository,
) : EventCollectLogPort {
    override fun save(log: EventCollectLog): EventCollectLog = eventCollectLogJpaRepository.saveAndFlush(log)

    override fun findAll(): List<EventCollectLog> = eventCollectLogJpaRepository.findAll()
}
