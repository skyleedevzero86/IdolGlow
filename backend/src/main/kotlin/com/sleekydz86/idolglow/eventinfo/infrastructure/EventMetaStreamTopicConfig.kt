package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.EventMetaStreamProperties
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.TopicPartition
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

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

    @Bean
    fun eventMetaDeadLetterTopic(properties: EventMetaStreamProperties): NewTopic =
        TopicBuilder
            .name(properties.deadLetterTopic)
            .partitions(PARTITIONS)
            .replicas(1)
            .build()

    @Bean
    fun eventMetaListenerErrorHandler(
        kafkaTemplate: KafkaTemplate<String, String>,
        properties: EventMetaStreamProperties,
    ): DefaultErrorHandler {
        val recoverer =
            DeadLetterPublishingRecoverer(kafkaTemplate) { record, _ ->
                if (record.topic() == properties.deadLetterTopic) {
                    null
                } else {
                    TopicPartition(properties.deadLetterTopic, record.partition())
                }
            }
        return DefaultErrorHandler(recoverer, FixedBackOff(properties.retryIntervalMs, properties.maxAttempts - 1))
    }

    companion object {
        private const val PARTITIONS: Int = 3
    }
}
