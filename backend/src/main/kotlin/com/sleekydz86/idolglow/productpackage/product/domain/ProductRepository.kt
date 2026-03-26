package com.sleekydz86.idolglow.productpackage.product.domain

import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSpecificResponse


interface ProductRepository {
    fun findProductsByNoOffset(
        lastId: Long?,
        size: Int,
        tagName: String?,
    ): List<ProductPagingQueryResponse>
    fun findProductSpecificById(productId: Long): ProductSpecificResponse?
    fun findById(productId: Long): Product?
    fun existsById(productId: Long): Boolean
    fun getReferenceById(productId: Long): Product
}