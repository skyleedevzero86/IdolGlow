package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.EventMetaStreamProperties
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
@ConditionalOnProperty(prefix = "app.event-meta.stream", name = ["enabled"], havingValue = "true")
class EventMetaStreamTopicConfig {
    @Bean
    fun eventMetaChangedTopic(properties: EventMetaStreamProperties): NewTopic =
        TopicBuilder
            .name(properties.topic)
            .partitions(PARTITIONS)
            .replicas(1)
            .build()

    companion object {
        private const val PARTITIONS: Int = 3
    }
}
