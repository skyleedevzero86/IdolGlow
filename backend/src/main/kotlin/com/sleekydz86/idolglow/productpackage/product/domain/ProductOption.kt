package com.sleekydz86.idolglow.productpackage.product.domain

import com.sleekydz86.idolglow.global.BaseEntity
import com.sleekydz86.idolglow.productpackage.option.domain.Option
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
    name = "product_option",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_product_option", columnNames = ["product_id", "option_id"])
    ]
)
class ProductOption(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    val product: Product,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id")
    val option: Option
) : BaseEntity()
