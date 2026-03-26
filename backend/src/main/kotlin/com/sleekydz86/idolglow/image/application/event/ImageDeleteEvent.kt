package com.sleekydz86.idolglow.image.application.event

data class ImageDeleteEvent(
    val aggregateType: ImageAggregateType,
    val aggregateId: Long,
)
