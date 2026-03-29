package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class ProductCommandRepository(
    private val productJpaRepository: ProductJpaRepository
) {

    fun findById(productId: Long): Product? = productJpaRepository.findByIdOrNull(productId)

    fun save(product: Product): Product = productJpaRepository.save(product)

    fun existsById(productId: Long): Boolean = productJpaRepository.existsById(productId)

    fun getReferenceById(productId: Long): Product = productJpaRepository.getReferenceById(productId)

    fun delete(product: Product) = productJpaRepository.delete(product)
}
