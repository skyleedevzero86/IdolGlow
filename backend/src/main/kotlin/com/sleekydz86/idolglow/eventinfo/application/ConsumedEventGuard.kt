package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.ConsumedEventPort
import com.sleekydz86.idolglow.eventinfo.domain.ConsumedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class ConsumedEventGuard(
    private val consumedEventPort: ConsumedEventPort,
) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun firstDelivery(
        consumerName: String,
        eventId: String,
    ): ConsumedEvent? {
        if (consumedEventPort.exists(consumerName, eventId)) return null
        return consumedEventPort.save(
            ConsumedEvent(consumerName = consumerName, eventId = eventId, processedAt = LocalDateTime.now()),
        )
    }
}
