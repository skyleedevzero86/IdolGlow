package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ProductJpaRepository : JpaRepository<Product, Long> {
    @Modifying
    @Query(
        value = "UPDATE products SET wish_count = wish_count + :delta WHERE id = :productId",
        nativeQuery = true,
    )
    fun addWishCount(
        productId: Long,
        delta: Long,
    ): Int

    @Modifying
    @Query(
        value =
            "UPDATE products SET review_count = review_count + :reviewCountDelta, " +
                "rating_sum = rating_sum + :ratingSumDelta WHERE id = :productId",
        nativeQuery = true,
    )
    fun addReviewAggregate(
        productId: Long,
        reviewCountDelta: Long,
        ratingSumDelta: Long,
    ): Int

    @Modifying
    @Query(
        value =
            "UPDATE products SET wish_count = wish_count - " +
                "(SELECT COUNT(*) FROM wishes w WHERE w.product_id = products.id AND w.user_id = :userId) " +
                "WHERE id IN (SELECT w2.product_id FROM wishes w2 WHERE w2.user_id = :userId)",
        nativeQuery = true,
    )
    fun subtractWishCountsOfUser(userId: Long): Int
}
