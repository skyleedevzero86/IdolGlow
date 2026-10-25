package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.EventCollectLog
import org.springframework.data.jpa.repository.JpaRepository

interface EventCollectLogJpaRepository : JpaRepository<EventCollectLog, Long>
