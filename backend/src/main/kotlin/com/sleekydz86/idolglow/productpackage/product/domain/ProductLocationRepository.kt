package com.sleekydz86.idolglow.productpackage.product.domain

import org.springframework.stereotype.Repository

@Repository
interface ProductLocationRepository {
    fun save(location: ProductLocation): ProductLocation
    fun findByProductId(productId: Long): ProductLocation?
}
