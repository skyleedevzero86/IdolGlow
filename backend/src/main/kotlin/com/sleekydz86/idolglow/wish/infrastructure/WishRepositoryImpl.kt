package com.sleekydz86.idolglow.wish.infrastructure

import com.sleekydz86.idolglow.wish.application.dto.WishedProductPagingResponse
import com.sleekydz86.idolglow.wish.domain.Wish
import com.sleekydz86.idolglow.wish.domain.WishRepository
import org.springframework.stereotype.Repository

@Repository
class WishRepositoryImpl(
    private val wishCommandRepository: WishCommandRepository,
    private val wishQueryRepository: WishQueryRepository
) : WishRepository {
    override fun save(wish: Wish): Wish =
        wishCommandRepository.save(wish)

    override fun findById(wishId: Long): Wish? =
        wishCommandRepository.findById(wishId)

    override fun delete(wish: Wish) =
        wishCommandRepository.delete(wish)

    override fun findByUserIdAndProductId(userId: Long, productId: Long): Wish? =
        wishCommandRepository.findByUserIdAndProductId(userId, productId)

    override fun findByWishedProductsByNoOffset(
        userId: Long,
        lastWishId: Long?,
        size: Int
    ): List<WishedProductPagingResponse> =
        wishQueryRepository.findByWishedProductsByNoOffset(userId, lastWishId, size)

    override fun deleteAllByUserId(userId: Long) =
        wishCommandRepository.deleteByUserId(userId)

    override fun deleteAllByProductId(productId: Long) =
        wishCommandRepository.deleteByProductId(productId)
}
