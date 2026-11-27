package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductEventPriceType
import com.sleekydz86.idolglow.support.MysqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("상품 이벤트 저장·조회 (MySQL)")
class ProductEventSaleMysqlTest : MysqlIntegrationTest() {
    @Autowired private lateinit var productCommandRepository: ProductCommandRepository

    @Autowired private lateinit var transactionManager: PlatformTransactionManager

    @DisplayName("할인율 이벤트와 이벤트 기본가 이벤트를 저장하고 다시 읽으면 값이 같고, 지우면 비어 있다")
    @Test
    fun roundTripsEventSale() {
        // given
        val rateId = save(ProductEventPriceType.DISCOUNT_RATE, 99, null)
        val fixedId = save(ProductEventPriceType.FIXED_BASE_PRICE, null, BigDecimal("9999"))

        // when
        val rate = read(rateId)
        val fixed = read(fixedId)
        transaction {
            val product = requireNotNull(productCommandRepository.findById(fixedId))
            product.clearEventSale()
            productCommandRepository.save(product)
        }
        val cleared = read(fixedId)

        // then
        assertEquals(listOf<Any?>(START, END, ProductEventPriceType.DISCOUNT_RATE, 99, null), rate)
        assertEquals(
            listOf<Any?>(START, END, ProductEventPriceType.FIXED_BASE_PRICE, null, BigDecimal("9999.00")),
            fixed,
        )
        assertNull(cleared)
    }

    private fun save(
        priceType: ProductEventPriceType,
        discountRate: Int?,
        eventBasePrice: BigDecimal?,
    ): Long {
        var id = 0L
        transaction {
            val product = Product(name = "이벤트", description = "설명", basePrice = BigDecimal("10000.00"))
            product.changeEventSale(START, END, priceType, discountRate, eventBasePrice)
            id = productCommandRepository.save(product).id
        }
        return id
    }

    private fun read(id: Long): List<Any?>? {
        var values: List<Any?>? = null
        transaction {
            val sale = requireNotNull(productCommandRepository.findById(id)).eventSale
            values = sale?.let { listOf(it.startsOn, it.endsOn, it.priceType, it.discountRate, it.eventBasePrice) }
        }
        return values
    }

    private fun transaction(block: () -> Unit) {
        TransactionTemplate(transactionManager).executeWithoutResult { block() }
    }

    companion object {
        private val START = LocalDate.of(2026, 12, 1)
        private val END = LocalDate.of(2026, 12, 31)
    }
}
