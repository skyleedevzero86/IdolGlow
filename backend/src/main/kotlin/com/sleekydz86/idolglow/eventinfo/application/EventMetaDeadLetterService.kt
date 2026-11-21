package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMetaDeadLetterPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaApplyResult
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaDeadLetter
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaMessage
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.eventinfo.EventMetaDeadLetterExceptionType
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

@Service
class EventMetaDeadLetterService(
    private val eventMetaDeadLetterPort: EventMetaDeadLetterPort,
    private val eventMetaProjectionService: EventMetaProjectionService,
    private val eventInterestAlertService: EventInterestAlertService,
    private val eventSearchIndexService: EventSearchIndexService,
    private val eventMetaDeadLetterTxService: EventMetaDeadLetterTxService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun record(failed: FailedEventMetaRecord): Boolean {
        val eventId = readEventId(failed.payload) ?: failed.fallbackEventId
        if (eventMetaDeadLetterPort.exists(failed.consumerName, eventId)) return false
        eventMetaDeadLetterPort.save(
            EventMetaDeadLetter(
                consumerName = failed.consumerName,
                eventId = eventId,
                payload = failed.payload,
                errorType = failed.errorType,
                failedAt = LocalDateTime.now(),
            ),
        )
        log.warn(
            "공연 메타 이벤트 처리가 계속 실패해 실패 목록에 남깁니다. consumer={} eventId={} error={}",
            failed.consumerName,
            eventId,
            failed.errorType,
        )
        return true
    }

    @Transactional(readOnly = true)
    fun list(pendingOnly: Boolean): List<EventMetaDeadLetterView> =
        eventMetaDeadLetterPort.findRecent(pendingOnly).map(EventMetaDeadLetterView::from)

    fun reprocess(id: Long): EventMetaDeadLetterReprocessed {
        val deadLetter = pending(id)
        val handler =
            handlerFor(deadLetter.consumerName)
                ?: throw CustomException(EventMetaDeadLetterExceptionType.UNKNOWN_CONSUMER)
        val result = runHandler(deadLetter, handler)
        return EventMetaDeadLetterReprocessed(eventMetaDeadLetterTxService.markReprocessed(id), result)
    }

    private fun pending(id: Long): EventMetaDeadLetter {
        val deadLetter =
            eventMetaDeadLetterPort.findById(id)
                ?: throw CustomException(EventMetaDeadLetterExceptionType.NOT_FOUND)
        if (deadLetter.isReprocessed()) throw CustomException(EventMetaDeadLetterExceptionType.ALREADY_REPROCESSED)
        return deadLetter
    }

    private fun runHandler(
        deadLetter: EventMetaDeadLetter,
        handler: (EventMetaMessage) -> EventMetaApplyResult,
    ): EventMetaApplyResult =
        try {
            handler(objectMapper.readValue(deadLetter.payload, EventMetaMessage::class.java))
        } catch (e: DataAccessException) {
            throw reprocessFailed(deadLetter, e)
        } catch (e: JacksonException) {
            throw reprocessFailed(deadLetter, e)
        } catch (e: IllegalArgumentException) {
            throw reprocessFailed(deadLetter, e)
        } catch (e: IllegalStateException) {
            throw reprocessFailed(deadLetter, e)
        }

    private fun reprocessFailed(
        deadLetter: EventMetaDeadLetter,
        cause: RuntimeException,
    ): CustomException {
        log.warn(
            "실패한 공연 메타 이벤트를 다시 처리하지 못했습니다. id={} consumer={} eventId={}",
            deadLetter.id,
            deadLetter.consumerName,
            deadLetter.eventId,
            cause,
        )
        return CustomException(EventMetaDeadLetterExceptionType.REPROCESS_FAILED)
    }

    private fun handlerFor(consumerName: String): ((EventMetaMessage) -> EventMetaApplyResult)? =
        when (consumerName) {
            EventMetaProjectionService.CONSUMER -> eventMetaProjectionService::apply
            EventInterestAlertService.CONSUMER -> eventInterestAlertService::handle
            EventSearchIndexService.CONSUMER -> eventSearchIndexService::handle
            else -> null
        }

    private fun readEventId(payload: String): String? =
        try {
            objectMapper.readValue(payload, EventMetaMessage::class.java).eventId.ifBlank { null }
        } catch (e: JacksonException) {
            log.warn("실패 메시지에서 이벤트 ID를 읽지 못했습니다. error={}", e.javaClass.simpleName)
            null
        }

    companion object {
        const val CONSUMER: String = "event-meta-dead-letter"
    }
}

data class FailedEventMetaRecord(
    val consumerName: String,
    val payload: String,
    val errorType: String?,
    val fallbackEventId: String,
)

data class EventMetaDeadLetterView(
    val id: Long,
    val consumerName: String,
    val eventId: String,
    val errorType: String?,
    val failedAt: LocalDateTime,
    val reprocessedAt: LocalDateTime?,
) {
    companion object {
        fun from(deadLetter: EventMetaDeadLetter): EventMetaDeadLetterView =
            EventMetaDeadLetterView(
                id = deadLetter.id,
                consumerName = deadLetter.consumerName,
                eventId = deadLetter.eventId,
                errorType = deadLetter.errorType,
                failedAt = deadLetter.failedAt,
                reprocessedAt = deadLetter.reprocessedAt,
            )
    }
}

data class EventMetaDeadLetterReprocessed(
    val deadLetter: EventMetaDeadLetterView,
    val result: EventMetaApplyResult,
)
