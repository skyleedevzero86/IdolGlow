package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductLocation
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal

@DisplayName("상품 목록 조회 쿼리 수")
class ProductBrowseQueryPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var productJpaRepository: ProductJpaRepository

    @Autowired private lateinit var productQueryRepository: ProductQueryRepository

    @Autowired private lateinit var entityManagerFactory: EntityManagerFactory

    @Autowired private lateinit var transactionTemplate: TransactionTemplate

    @DisplayName("상품 수와 상관없이 장소를 상품과 한 번에 읽어, 상품 4개에 SQL 2번(상품·장소, 태그)만 쓴다")
    @Test
    fun locationsAreFetchedWithProductsWithoutNPlusOne() {
        // given
        val withLocation = (1..3).map { 상품("장소 있는 상품 $it", hasLocation = true) }
        val withoutLocation = 상품("장소 없는 상품", hasLocation = false)
        val ids = withLocation.map { it.id } + withoutLocation.id
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.isStatisticsEnabled = true
        statistics.clear()

        // when
        val items = 조회(ids)
        val statements = statistics.prepareStatementCount
        statistics.isStatisticsEnabled = false

        // then
        assertEquals(2L, statements)
        assertEquals(ids, items.map { it.id })
        assertEquals(listOf("장소 1", "장소 2", "장소 3"), items.take(3).map { it.location?.name })
        assertNull(items.last().location)
        assertNotNull(items.first().distanceMeters)
        assertTrue(items.first().distanceMeters!! < 1.0)
    }

    private fun 조회(ids: List<Long>): List<ProductPagingQueryResponse> =
        checkNotNull(
            transactionTemplate.execute {
                productQueryRepository.hydrateForBrowse(
                    orderedIds = ids,
                    wishCounts = emptyMap(),
                    reviewMetrics = emptyMap(),
                    nearLatitude = BigDecimal("37.5665000"),
                    nearLongitude = BigDecimal("126.9780000"),
                )
            },
        )

    private fun 상품(
        name: String,
        hasLocation: Boolean,
    ): Product {
        val product = Product(name = name, description = "$name 설명")
        if (hasLocation) {
            val index = name.last()
            product.setLocation(
                ProductLocation(
                    product = product,
                    name = "장소 $index",
                    latitude = BigDecimal("37.5665000"),
                    longitude = BigDecimal("126.9780000"),
                    roadAddressName = "서울 중구 세종대로 110",
                    addressName = null,
                    kakaoPlaceId = "kakao-$index",
                ),
            )
        }
        return productJpaRepository.saveAndFlush(product)
    }
}
