package com.sleekydz86.idolglow.wish.application

import com.sleekydz86.idolglow.wish.application.event.WishDeleteEvent
import com.sleekydz86.idolglow.wish.domain.vo.WishAggregateType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class WishEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun publishDelete(
        aggregateType: WishAggregateType,
        aggregateId: Long,
    ) {
        publishDelete(
            WishDeleteEvent(
                aggregateType = aggregateType,
                aggregateId = aggregateId
            )
        )
    }

    private fun publishDelete(event: WishDeleteEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
