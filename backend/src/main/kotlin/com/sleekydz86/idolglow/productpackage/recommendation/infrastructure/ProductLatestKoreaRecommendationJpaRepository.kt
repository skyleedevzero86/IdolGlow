package com.sleekydz86.idolglow.productpackage.recommendation.infrastructure

import com.sleekydz86.idolglow.productpackage.recommendation.domain.ProductLatestKoreaRecommendation
import org.springframework.data.jpa.repository.JpaRepository

interface ProductLatestKoreaRecommendationJpaRepository : JpaRepository<ProductLatestKoreaRecommendation, Long> {
    fun findAllByOrderByDisplayOrderAscIdAsc(): List<ProductLatestKoreaRecommendation>
}
