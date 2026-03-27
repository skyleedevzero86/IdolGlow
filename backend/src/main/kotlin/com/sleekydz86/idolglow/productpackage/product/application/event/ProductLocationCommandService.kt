package com.sleekydz86.idolglow.productpackage.product.application.event

import com.sleekydz86.idolglow.productpackage.product.application.dto.ProductLocationPayload
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductLocation
import com.sleekydz86.idolglow.productpackage.product.domain.ProductLocationRepository
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductCommandRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class ProductLocationCommandService(
    private val productCommandRepository: ProductCommandRepository,
    private val productLocationRepository: ProductLocationRepository,
) {

    fun upsertProductLocation(productId: Long, payload: ProductLocationPayload) {
        val product = findProduct(productId)
        val location = productLocationRepository.findByProductId(productId)

        if (location != null) {
            location.update(payload)
            if (!product.hasLocation()) {
                product.setLocation(location)
            }
            return
        }

        val newLocation = ProductLocation.of(product, payload)
        product.setLocation(newLocation)
        productLocationRepository.save(newLocation)
    }

    private fun findProduct(productId: Long): Product {
        return productCommandRepository.findById(productId)
            ?: throw IllegalArgumentException("상품을 찾을 수 없습니다. productId=$productId")
    }
}
