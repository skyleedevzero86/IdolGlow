package com.sleekydz86.idolglow.productpackage.product.application

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.image.application.AggregateImageQueryService
import com.sleekydz86.idolglow.productpackage.product.application.dto.ProductBrowseParams
import com.sleekydz86.idolglow.productpackage.product.domain.ProductRepository
import com.sleekydz86.idolglow.productpackage.product.domain.ProductSort
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductBrowseResult
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSearchCriteria
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("상품 목록 조회의 현재 시각")
class ProductQueryServiceKoreaTimeTest {
    private val productRepository = mockk<ProductRepository>()
    private val images = mockk<AggregateImageQueryService>()
    private val service = ProductQueryService(productRepository, images)

    @DisplayName("상품 목록 조건에 한국 현재 시각과 한국 오늘 날짜를 넣는다")
    @Test
    fun browseUsesKoreaNow() {
        // given
        val criteria = slot<ProductSearchCriteria>()
        every { productRepository.browseProducts(capture(criteria)) } returns
            ProductBrowseResult(emptyList(), null, null)
        every { images.firstProductImageUrlByProductIds(any()) } returns emptyMap()

        // when
        service.browseProducts(params())

        // then
        val gap = Duration.between(criteria.captured.now, LocalDateTime.now(IdolGlowDateTimes.ZONE)).abs()
        assertTrue(gap < Duration.ofMinutes(1), "한국 시각과 차이=$gap")
        assertEquals(LocalDate.now(IdolGlowDateTimes.ZONE), criteria.captured.today)
    }

    private fun params(): ProductBrowseParams =
        ProductBrowseParams(
            lastId = null,
            offset = null,
            size = 10,
            tag = null,
            tags = emptyList(),
            keyword = null,
            minPrice = null,
            maxPrice = null,
            visitDate = null,
            reservableOnly = false,
            sort = ProductSort.NEWEST,
            nearLatitude = null,
            nearLongitude = null,
            radiusMeters = null,
        )
}
