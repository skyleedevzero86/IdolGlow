package com.sleekydz86.idolglow.productpackage.recommendation.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RecommendationProductJpaRepository : JpaRepository<Product, Long> {
    fun findByIsRecommendedTrueOrderByRecommendationScoreDescIdDesc(pageable: Pageable): List<Product>

    @Query(
        """
        select distinct p
        from Product p
        left join p.productTags t
        where p.isRecommended = true
          and (:tag is null or lower(t.tagName) = lower(:tag))
          and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))
        order by p.recommendationScore desc, p.id desc
        """
    )
    fun findAdminPicked(
        @Param("tag") tag: String?,
        @Param("keyword") keyword: String?,
        pageable: Pageable,
    ): List<Product>
}
