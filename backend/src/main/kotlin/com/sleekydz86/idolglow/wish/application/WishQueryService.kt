package com.sleekydz86.idolglow.wish.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class WishQueryService(
    private val wishRepository: WishRepository
) {
    fun findWishedProductByNoOffset(
        userId: Long,
        lastWishId: Long?,
        size: Int
    ): List<WishedProductPagingResponse> =
        wishRepository.findByWishedProductsByNoOffset(
            userId = userId,
            lastWishId = lastWishId,
            size = size,
        )
}
