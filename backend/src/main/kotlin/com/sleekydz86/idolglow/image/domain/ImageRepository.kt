package com.sleekydz86.idolglow.image.domain

interface ImageRepository {
    fun save(image: Image): Image
    fun saveAll(images: Collection<Image>): List<Image>
    fun findByAggregate(aggregateType: ImageAggregateType, aggregateId: Long): List<Image>
    fun findByAggregates(aggregateType: ImageAggregateType, aggregateIds: Collection<Long>): List<Image>
    fun deleteAllByAggregate(aggregateType: ImageAggregateType, aggregateId: Long)
}
