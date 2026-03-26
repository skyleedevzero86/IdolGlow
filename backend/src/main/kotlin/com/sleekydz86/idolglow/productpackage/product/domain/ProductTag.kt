package com.sleekydz86.idolglow.productpackage.product.domain

import com.sleekydz86.idolglow.global.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Index

@Entity
@Table(
    name = "product_tag",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_product_tag", columnNames = ["product_id", "tag_name"])
    ],
    indexes = [
        Index(name = "idx_product_tag_tag_name", columnList = "tag_name"),
        Index(name = "idx_product_tag_product_id", columnList = "product_id"),
    ]
)
class ProductTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    val product: Product,

    @Column(name = "tag_name", nullable = false, length = 50)
    val tagName: String
) : BaseEntity()
