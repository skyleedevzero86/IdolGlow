package com.sleekydz86.idolglow.image.application.event

import com.sleekydz86.idolglow.image.domain.vo.ImageAggregateType

data class ImageDeleteEvent(
    val aggregateType: ImageAggregateType,
    val aggregateId: Long,
)
