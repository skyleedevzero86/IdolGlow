package com.sleekydz86.idolglow.eventinfo.adapter.kafka

import com.sleekydz86.idolglow.eventinfo.application.EventInterestAlertService
import com.sleekydz86.idolglow.eventinfo.application.EventMetaProjectionService
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaMessage
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
@ConditionalOnProperty(prefix = "app.event-meta.stream", name = ["enabled"], havingValue = "true")
class EventMetaKafkaListener(
    private val objectMapper: ObjectMapper,
    private val eventMetaProjectionService: EventMetaProjectionService,
    private val eventInterestAlertService: EventInterestAlertService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${app.event-meta.stream.topic:event-meta.changed}"],
        groupId = EventMetaProjectionService.CONSUMER,
    )
    fun onProjection(payload: String) {
        val message = read(payload)
        val result = eventMetaProjectionService.apply(message)
        log.debug("공연 메타 읽기 모델 반영. eventId={} result={}", message.eventId, result)
    }

    @KafkaListener(
        topics = ["\${app.event-meta.stream.topic:event-meta.changed}"],
        groupId = EventInterestAlertService.CONSUMER,
    )
    fun onInterestAlert(payload: String) {
        val message = read(payload)
        val result = eventInterestAlertService.handle(message)
        log.debug("관심 공연 알림 처리. eventId={} result={}", message.eventId, result)
    }

    private fun read(payload: String): EventMetaMessage = objectMapper.readValue(payload, EventMetaMessage::class.java)
}
