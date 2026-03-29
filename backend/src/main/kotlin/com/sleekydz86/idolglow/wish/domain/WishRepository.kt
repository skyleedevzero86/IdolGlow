package com.sleekydz86.idolglow.wish.domain

import com.sleekydz86.idolglow.wish.application.dto.WishedProductPagingResponse

interface WishRepository {
    fun save(wish: Wish): Wish
    fun findById(wishId: Long): Wish?
    fun findByUserIdAndProductId(userId: Long, productId: Long): Wish?
    fun delete(wish: Wish)
    fun findByWishedProductsByNoOffset(
        userId: Long,
        lastWishId: Long?,
        size: Int,
    ): List<WishedProductPagingResponse>
    fun deleteAllByUserId(userId: Long)
    fun deleteAllByProductId(productId: Long)
}
