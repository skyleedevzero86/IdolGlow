package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.eventinfo.domain.EventCollectLog

interface EventCollectLogPort {
    fun save(log: EventCollectLog): EventCollectLog

    fun findAll(): List<EventCollectLog>
}
