package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductRepository
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSpecificResponse
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val productCommandRepository: ProductCommandRepository,
    private val productQueryRepository: ProductQueryRepository
) : ProductRepository {

    override fun findProductsByNoOffset(
        lastId: Long?,
        size: Int,
        tagName: String?,
    ): List<ProductPagingQueryResponse> =
        productQueryRepository.findProductsByNoOffset(
            lastId = lastId,
            size = size,
            tagName = tagName
        )

    override fun findProductSpecificById(productId: Long): ProductSpecificResponse? =
        productQueryRepository.findProductSpecificById(productId)

    override fun findById(productId: Long): Product? {
        return productCommandRepository.findById(productId)
    }

    override fun existsById(productId: Long): Boolean =
        productCommandRepository.existsById(productId)

    override fun getReferenceById(productId: Long): Product =
        productCommandRepository.getReferenceById(productId)
}
