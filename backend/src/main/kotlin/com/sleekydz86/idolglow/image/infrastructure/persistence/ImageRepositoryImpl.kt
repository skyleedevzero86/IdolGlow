package com.sleekydz86.idolglow.image.infrastructure.persistence

import com.sleekydz86.idolglow.image.domain.Image
import com.sleekydz86.idolglow.image.domain.ImageRepository
import com.sleekydz86.idolglow.image.domain.vo.ImageAggregateType
import org.springframework.stereotype.Repository

@Repository
class ImageRepositoryImpl(
    private val imageJpaRepository: ImageJpaRepository
) : ImageRepository {

    override fun save(image: Image): Image =
        imageJpaRepository.save(image)

    override fun saveAll(images: Collection<Image>): List<Image> =
        imageJpaRepository.saveAll(images)

    override fun findByAggregate(aggregateType: ImageAggregateType, aggregateId: Long): List<Image> =
        imageJpaRepository.findByAggregateTypeAndAggregateIdOrderBySortOrderAsc(aggregateType, aggregateId)

    override fun findByAggregates(aggregateType: ImageAggregateType, aggregateIds: Collection<Long>): List<Image> =
        if (aggregateIds.isEmpty()) emptyList()
        else imageJpaRepository.findByAggregateTypeAndAggregateIdInOrderBySortOrderAsc(aggregateType, aggregateIds)

    override fun deleteAllByAggregate(aggregateType: ImageAggregateType, aggregateId: Long) {
        imageJpaRepository.deleteByAggregateTypeAndAggregateId(aggregateType, aggregateId)
    }
}
