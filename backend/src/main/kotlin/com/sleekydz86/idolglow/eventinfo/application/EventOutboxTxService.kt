package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventOutboxPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class EventOutboxTxService(
    private val eventOutboxPort: EventOutboxPort,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markSent(outboxId: Long) {
        eventOutboxPort.findById(outboxId)?.markSent(LocalDateTime.now())
    }
}
