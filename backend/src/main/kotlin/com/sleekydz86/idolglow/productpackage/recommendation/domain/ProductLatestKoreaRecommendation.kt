package com.sleekydz86.idolglow.productpackage.recommendation.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "product_latest_korea_recommendation")
class ProductLatestKoreaRecommendation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int,
    @Column(name = "product_id", nullable = false)
    var productId: Long,
) : BaseEntity()
