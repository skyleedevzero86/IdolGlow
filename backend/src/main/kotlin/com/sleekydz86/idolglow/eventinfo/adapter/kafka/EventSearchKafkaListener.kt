package com.sleekydz86.idolglow.eventinfo.adapter.kafka

import com.sleekydz86.idolglow.eventinfo.application.EventSearchIndexService
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaMessage
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
@ConditionalOnExpression("\${app.event-meta.stream.enabled:false} and \${app.event-search.enabled:false}")
class EventSearchKafkaListener(
    private val objectMapper: ObjectMapper,
    private val eventSearchIndexService: EventSearchIndexService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${app.event-meta.stream.topic:event-meta.changed}"],
        groupId = EventSearchIndexService.CONSUMER,
    )
    fun onSearchIndex(payload: String) {
        val message = objectMapper.readValue(payload, EventMetaMessage::class.java)
        val result = eventSearchIndexService.handle(message)
        log.debug("공연 검색 색인 반영. eventId={} result={}", message.eventId, result)
    }
}
