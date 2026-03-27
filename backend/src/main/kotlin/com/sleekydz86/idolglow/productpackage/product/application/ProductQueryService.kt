package com.sleekydz86.idolglow.productpackage.product.application

import com.sleekydz86.idolglow.productpackage.product.domain.ProductRepository
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSpecificResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Transactional(readOnly = true)
@Service
class ProductQueryService(
    private val productRepository: ProductRepository,
) {

    fun findProductsByNoOffset(
        lastId: Long?,
        size: Int,
        tagName: String?,
    ): List<ProductPagingQueryResponse> =
        productRepository.findProductsByNoOffset(
            lastId = lastId,
            size = size,
            tagName = tagName
        )

    fun findProductSpecificById(productId: Long): ProductSpecificResponse {
        return productRepository.findProductSpecificById(productId)
            ?: throw IllegalArgumentException("상품을 찾을 수 없습니다. productId=$productId")
    }
}
