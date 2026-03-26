package com.sleekydz86.idolglow.image.infrastructure.persistence

import com.sleekydz86.idolglow.image.domain.Image
import com.sleekydz86.idolglow.image.domain.vo.ImageAggregateType
import org.springframework.data.jpa.repository.JpaRepository

interface ImageJpaRepository : JpaRepository<Image, Long> {

    fun findByAggregateTypeAndAggregateIdOrderBySortOrderAsc(
        aggregateType: ImageAggregateType,
        aggregateId: Long
    ): List<Image>

    fun findByAggregateTypeAndAggregateIdInOrderBySortOrderAsc(
        aggregateType: ImageAggregateType,
        aggregateIds: Collection<Long>
    ): List<Image>

    fun deleteByAggregateTypeAndAggregateId(aggregateType: ImageAggregateType, aggregateId: Long)
}
