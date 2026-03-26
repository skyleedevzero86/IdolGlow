package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.ProductLocation
import org.springframework.data.jpa.repository.JpaRepository

interface ProductLocationJpaRepository : JpaRepository<ProductLocation, Long> {
    fun findByProductId(productId: Long): ProductLocation?
}
