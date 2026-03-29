package com.sleekydz86.idolglow.wish.infrastructure

import com.sleekydz86.idolglow.wish.domain.Wish
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class WishCommandRepository(
    private val wishJpaRepository: WishJpaRepository
){

    fun findById(wishId: Long): Wish? = wishJpaRepository.findByIdOrNull(wishId)
    fun save(wish: Wish): Wish = wishJpaRepository.save(wish)
    fun delete(wish: Wish) = wishJpaRepository.delete(wish)
    fun findByUserIdAndProductId(userId: Long, productId: Long): Wish? =
        wishJpaRepository.findByUserIdAndProductId(userId, productId)
    fun deleteByUserId(userId: Long) =
        wishJpaRepository.deleteByUserId(userId)
    fun deleteByProductId(productId: Long) =
        wishJpaRepository.deleteByProductId(productId)
}
