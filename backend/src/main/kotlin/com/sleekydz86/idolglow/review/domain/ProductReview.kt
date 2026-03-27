package com.sleekydz86.idolglow.review.domain

import com.sleekydz86.idolglow.global.BaseEntity
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "product_reviews",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_product_review_user",
            columnNames = ["product_id", "user_id"]
        )
    ]
)
class ProductReview(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Embedded
    var rating: ReviewRating,

    @Column(nullable = false, length = 2000)
    var content: String,
) : BaseEntity() {

    fun validateOwner(userId: Long, productId: Long) {
        require(this.userId == userId) { "본인이 작성한 리뷰만 처리할 수 있습니다." }
        require(this.product.id == productId) { "해당 리뷰는 상품 ID $productId 에 속하지 않습니다." }
    }

    fun changeReview(rating: ReviewRating, content: String): ProductReview {
        validateContent(content)
        this.rating = rating
        this.content = content
        return this
    }

    companion object {
        fun of(
            product: Product,
            userId: Long,
            ratingScore:
            Int, content: String
        ): ProductReview {
            validateContent(content)
            return ProductReview(
                product = product,
                userId = userId,
                rating = ReviewRating.of(ratingScore),
                content = content
            )
        }

        private fun validateContent(content: String) {
            require(content.isNotBlank()) { "리뷰 내용은 비어 있을 수 없습니다." }
        }
    }
}
