package com.sleekydz86.idolglow.wish.domain

import com.sleekydz86.idolglow.global.BaseEntity
import com.sleekydz86.idolglow.user.user.domain.User
import jakarta.persistence.*

@Entity
@Table(
    name = "wishes",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_product_wish",
            columnNames = ["user_id", "product_id"]
        )
    ]
)
class Wish(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    val product: Product,
  ) : BaseEntity() {
}
