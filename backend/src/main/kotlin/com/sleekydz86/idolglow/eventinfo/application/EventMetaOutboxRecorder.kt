package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventOutboxPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaChangeType
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaMessage
import com.sleekydz86.idolglow.eventinfo.domain.EventOutbox
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import java.util.UUID

@Component
class EventMetaOutboxRecorder(
    private val eventOutboxPort: EventOutboxPort,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun record(
        master: EventMaster,
        type: EventMetaChangeType,
        changedFields: Set<String>,
    ): EventOutbox {
        val message =
            EventMetaMessage(
                eventId = UUID.randomUUID().toString(),
                eventType = type.name,
                globalId = master.globalId,
                metaVersion = master.nextMetaVersion(),
                occurredAt = OffsetDateTime.now().toString(),
                changedFields = changedFields.sorted(),
                snapshot = master.snapshot(),
            )
        return eventOutboxPort.save(
            EventOutbox(
                eventId = message.eventId,
                aggregateId = message.globalId,
                eventType = message.eventType,
                metaVersion = message.metaVersion,
                payload = objectMapper.writeValueAsString(message),
                schemaVersion = message.schemaVersion,
            ),
        )
    }
}
