package com.sleekydz86.idolglow.wish.application

import com.sleekydz86.idolglow.image.application.AggregateImageQueryService
import com.sleekydz86.idolglow.wish.application.dto.WishedProductPagingResponse
import com.sleekydz86.idolglow.wish.domain.WishRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class WishQueryService(
    private val wishRepository: WishRepository,
    private val aggregateImageQueryService: AggregateImageQueryService,
) {
    fun findWishedProductByNoOffset(
        userId: Long,
        lastWishId: Long?,
        size: Int
    ): List<WishedProductPagingResponse> {
        val list = wishRepository.findByWishedProductsByNoOffset(
            userId = userId,
            lastWishId = lastWishId,
            size = size,
        )
        val thumbs = aggregateImageQueryService.firstProductImageUrlByProductIds(list.map { it.id })
        return list.map { it.copy(thumbnailUrl = thumbs[it.id]) }
    }
}
