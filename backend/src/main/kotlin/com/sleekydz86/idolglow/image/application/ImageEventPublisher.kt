package com.sleekydz86.idolglow.image.application

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class ImageEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun publishCreate(event: ImageCreateEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    fun publishCreate(
        aggregateType: ImageAggregateType,
        aggregateId: Long,
        originalFilename: String,
        content: ByteArray,
        sortOrder: Int = 0
    ) {
        publishCreate(
            ImageCreateEvent(
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                originalFilename = originalFilename,
                content = content,
                sortOrder = sortOrder
            )
        )
    }

    fun publishDelete(
        aggregateType: ImageAggregateType,
        aggregateId: Long,
    ) {
        publishDelete(
            ImageDeleteEvent(
                aggregateType = aggregateType,
                aggregateId = aggregateId,
            )
        )
    }

    private fun publishDelete(event: ImageDeleteEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
