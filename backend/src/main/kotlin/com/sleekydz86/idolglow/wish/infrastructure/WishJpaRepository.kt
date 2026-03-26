package com.sleekydz86.idolglow.wish.infrastructure

import com.sleekydz86.idolglow.wish.domain.Wish
import org.springframework.data.jpa.repository.JpaRepository

interface WishJpaRepository: JpaRepository<Wish, Long> {
    fun findByUserIdAndProductId(userId: Long, productId: Long): Wish?
    fun deleteByUserId(userId: Long)
    fun deleteByProductId(productId: Long)
}