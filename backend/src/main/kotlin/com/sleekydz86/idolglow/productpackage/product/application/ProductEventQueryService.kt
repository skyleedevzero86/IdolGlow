package com.sleekydz86.idolglow.productpackage.product.application

import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductEventJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class ProductEventQueryService(
    private val productEventJpaRepository: ProductEventJpaRepository,
    private val productEventCalendar: ProductEventCalendar,
    private val productPagingResponseAssembler: ProductPagingResponseAssembler,
) {
    fun findActiveEvents(
        size: Int,
        tag: String?,
    ): List<ProductPagingQueryResponse> {
        require(size in MIN_SIZE..MAX_SIZE) { "조회 개수는 1 이상 100 이하여야 합니다." }
        val normalizedTag = tag?.trim()?.takeIf { it.isNotEmpty() }
        val today = productEventCalendar.today()
        val page = PageRequest.of(0, size)
        val products =
            if (normalizedTag == null) {
                productEventJpaRepository.findActiveEvents(today, page)
            } else {
                productEventJpaRepository.findActiveEventsByTag(today, normalizedTag, page)
            }
        return productPagingResponseAssembler.assemble(products)
    }

    companion object {
        const val DEFAULT_SIZE = 20
        private const val MIN_SIZE = 1
        private const val MAX_SIZE = 100
    }
}
