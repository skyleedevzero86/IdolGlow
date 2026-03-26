package com.sleekydz86.idolglow.image.application.event

data class ImageCreateEvent(
    val aggregateType: ImageAggregateType,
    val aggregateId: Long,
    val originalFilename: String,
    val content: ByteArray,
    val sortOrder: Int = 0,
)
