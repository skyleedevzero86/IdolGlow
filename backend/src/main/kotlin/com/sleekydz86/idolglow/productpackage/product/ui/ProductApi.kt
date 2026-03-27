package com.sleekydz86.idolglow.productpackage.product.ui

import com.sleekydz86.idolglow.productpackage.product.application.dto.ProductCreatedResponse
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSpecificResponse
import com.sleekydz86.idolglow.productpackage.product.ui.request.CreateProductRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity

interface ProductApi {
    fun findProducts(
        lastId: Long?,
        size: Int?,
        tag: String?,
    ): ResponseEntity<List<ProductPagingQueryResponse>>

    fun findProduct(productId: Long): ResponseEntity<ProductSpecificResponse>

    fun createProduct(@Valid request: CreateProductRequest): ResponseEntity<ProductCreatedResponse>
}
