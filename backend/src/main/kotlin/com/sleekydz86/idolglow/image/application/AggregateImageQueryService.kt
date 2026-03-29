package com.sleekydz86.idolglow.image.application

import com.sleekydz86.idolglow.image.domain.ImageRepository
import com.sleekydz86.idolglow.image.domain.vo.ImageAggregateType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class AggregateImageQueryService(
    private val imageRepository: ImageRepository,
) {

    fun firstProductImageUrlByProductIds(productIds: Collection<Long>): Map<Long, String> {
        if (productIds.isEmpty()) return emptyMap()
        val rows = imageRepository.findByAggregates(ImageAggregateType.PRODUCT, productIds)
        return rows.groupBy { it.aggregateId }.mapNotNull { (id, list) ->
            list.minByOrNull { it.sortOrder }?.url?.let { url -> id to url }
        }.toMap()
    }

    fun orderedProductImageUrls(productId: Long): List<String> =
        imageRepository.findByAggregate(ImageAggregateType.PRODUCT, productId).map { it.url }

    fun orderedOptionImageUrlsByOptionIds(optionIds: Collection<Long>): Map<Long, List<String>> {
        if (optionIds.isEmpty()) return emptyMap()
        val rows = imageRepository.findByAggregates(ImageAggregateType.OPTION, optionIds)
        return rows.groupBy { it.aggregateId }.mapValues { (_, list) ->
            list.sortedBy { it.sortOrder }.map { it.url }
        }
    }
}
