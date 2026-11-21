package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMetaDeadLetterPort
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.eventinfo.EventMetaDeadLetterExceptionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class EventMetaDeadLetterTxService(
    private val eventMetaDeadLetterPort: EventMetaDeadLetterPort,
) {
    @Transactional
    fun markReprocessed(id: Long): EventMetaDeadLetterView {
        val deadLetter =
            eventMetaDeadLetterPort.findById(id)
                ?: throw CustomException(EventMetaDeadLetterExceptionType.NOT_FOUND)
        if (!deadLetter.isReprocessed()) deadLetter.markReprocessed(LocalDateTime.now())
        return EventMetaDeadLetterView.from(deadLetter)
    }
}
