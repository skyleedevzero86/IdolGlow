package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.ProductLocation
import com.sleekydz86.idolglow.productpackage.product.domain.ProductLocationRepository
import org.springframework.stereotype.Repository

@Repository
class ProductLocationRepositoryImpl(
    private val productLocationJpaRepository: ProductLocationJpaRepository
) : ProductLocationRepository {
    override fun save(productLocation: ProductLocation): ProductLocation = productLocationJpaRepository.save(productLocation)
    override fun findByProductId(productId: Long): ProductLocation? =
        productLocationJpaRepository.findByProductId(productId)
}