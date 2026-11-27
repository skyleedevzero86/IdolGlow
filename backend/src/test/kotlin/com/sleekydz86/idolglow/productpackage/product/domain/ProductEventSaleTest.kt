package com.sleekydz86.idolglow.productpackage.product.domain

import com.sleekydz86.idolglow.productpackage.option.domain.Option
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("상품 이벤트 설정 규칙")
class ProductEventSaleTest {
    private val start = LocalDate.of(2026, 12, 1)
    private val end = LocalDate.of(2026, 12, 31)
    private val basePrice = BigDecimal("10000.00")

    @DisplayName("D1 시작일이 없으면 거절한다")
    @Test
    fun rejectsMissingStart() {
        // when
        val error = assertThrows<IllegalArgumentException> { rate(startsOn = null, rate = 30) }

        // then
        assertEquals(MISSING_MESSAGE, error.message)
    }

    @DisplayName("D1 종료일이 없으면 거절한다")
    @Test
    fun rejectsMissingEnd() {
        // when
        val error = assertThrows<IllegalArgumentException> { rate(endsOn = null, rate = 30) }

        // then
        assertEquals(MISSING_MESSAGE, error.message)
    }

    @DisplayName("D1 가격 방식이 없으면 거절한다")
    @Test
    fun rejectsMissingPriceType() {
        // when
        val error =
            assertThrows<IllegalArgumentException> {
                ProductEventSale.of(start, end, null, 30, null, basePrice)
            }

        // then
        assertEquals(MISSING_MESSAGE, error.message)
    }

    @DisplayName("D2 종료일이 시작일보다 하루 앞이면 거절한다")
    @Test
    fun rejectsEndBeforeStart() {
        // when
        val error = assertThrows<IllegalArgumentException> { rate(endsOn = start.minusDays(1), rate = 30) }

        // then
        assertEquals("이벤트 종료일은 시작일과 같거나 뒤여야 합니다.", error.message)
    }

    @DisplayName("D2 시작일과 종료일이 같은 하루 이벤트는 허용한다")
    @Test
    fun allowsOneDayEvent() {
        // when
        val sale = rate(endsOn = start, rate = 30)

        // then
        assertEquals(start, sale.endsOn)
    }

    @DisplayName("D3 시작일이 과거여도 허용한다")
    @Test
    fun allowsPastStart() {
        // when
        val sale = rate(startsOn = LocalDate.of(2020, 1, 1), rate = 30)

        // then
        assertEquals(LocalDate.of(2020, 1, 1), sale.startsOn)
    }

    @DisplayName("D4 할인율이 1 미만이거나 99 초과면 거절한다")
    @ParameterizedTest
    @ValueSource(ints = [0, 100, -1])
    fun rejectsDiscountRateOutOfRange(discountRate: Int) {
        // when
        val error = assertThrows<IllegalArgumentException> { rate(rate = discountRate) }

        // then
        assertEquals(RATE_MESSAGE, error.message)
    }

    @DisplayName("D4 할인율 경계 1과 99는 허용한다")
    @ParameterizedTest
    @ValueSource(ints = [1, 99])
    fun allowsDiscountRateBoundary(discountRate: Int) {
        // when
        val sale = rate(rate = discountRate)

        // then
        assertEquals(discountRate, sale.discountRate)
    }

    @DisplayName("D4 할인율 방식에 할인율이 없으면 거절한다")
    @Test
    fun rejectsMissingDiscountRate() {
        // when
        val error = assertThrows<IllegalArgumentException> { rate(rate = null) }

        // then
        assertEquals(RATE_MESSAGE, error.message)
    }

    @DisplayName("D4 할인율 방식에 이벤트 기본가가 함께 오면 거절한다")
    @Test
    fun rejectsBasePriceWithDiscountRate() {
        // when
        val error =
            assertThrows<IllegalArgumentException> {
                ProductEventSale.of(start, end, ProductEventPriceType.DISCOUNT_RATE, 30, BigDecimal("5000"), basePrice)
            }

        // then
        assertEquals(MIXED_MESSAGE, error.message)
    }

    @DisplayName("D5 이벤트 기본가 방식에 할인율이 함께 오면 거절한다")
    @Test
    fun rejectsDiscountRateWithFixedPrice() {
        // when
        val error =
            assertThrows<IllegalArgumentException> {
                ProductEventSale.of(start, end, ProductEventPriceType.FIXED_BASE_PRICE, 30, BigDecimal("5000"), basePrice)
            }

        // then
        assertEquals(MIXED_MESSAGE, error.message)
    }

    @DisplayName("D5 이벤트 기본가가 상품 기본가와 같거나 음수거나 없으면 거절한다")
    @ParameterizedTest
    @ValueSource(strings = ["10000", "10000.00", "-1", ""])
    fun rejectsFixedPriceOutOfRange(value: String) {
        // when
        val error =
            assertThrows<IllegalArgumentException> {
                fixed(value.takeIf { it.isNotEmpty() }?.let(::BigDecimal))
            }

        // then
        assertEquals("이벤트 기본가는 0원 이상이고 상품 기본가보다 작아야 합니다.", error.message)
    }

    @DisplayName("D5 이벤트 기본가에 원 미만 자리가 있으면 거절한다")
    @Test
    fun rejectsFractionalFixedPrice() {
        // when
        val error = assertThrows<IllegalArgumentException> { fixed(BigDecimal("5000.5")) }

        // then
        assertEquals("이벤트 기본가는 원 단위 정수여야 합니다.", error.message)
    }

    @DisplayName("D5 이벤트 기본가 경계 0원과 상품 기본가보다 1원 작은 값은 허용한다")
    @ParameterizedTest
    @ValueSource(strings = ["0", "9999"])
    fun allowsFixedPriceBoundary(value: String) {
        // when
        val sale = fixed(BigDecimal(value))

        // then
        assertEquals(BigDecimal(value).setScale(2), sale.eventBasePrice)
    }

    @DisplayName("D6 상품 기본가가 0원이면 이벤트 기본가 방식을 거절한다")
    @Test
    fun rejectsFixedPriceWhenProductBasePriceIsZero() {
        // when
        val error =
            assertThrows<IllegalArgumentException> {
                ProductEventSale.of(
                    start,
                    end,
                    ProductEventPriceType.FIXED_BASE_PRICE,
                    null,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                )
            }

        // then
        assertEquals("상품 기본가가 0원이면 이벤트 기본가 방식을 쓸 수 없습니다.", error.message)
    }

    @DisplayName("상태는 시작일 전날 SCHEDULED, 시작일과 종료일 ACTIVE, 종료일 다음 날 ENDED")
    @Test
    fun statusFollowsInclusivePeriod() {
        // given
        val sale = rate(rate = 30)

        // when
        val statuses = listOf(start.minusDays(1), start, end, end.plusDays(1)).map { sale.status(it) }

        // then
        assertEquals(
            listOf(
                ProductEventStatus.SCHEDULED,
                ProductEventStatus.ACTIVE,
                ProductEventStatus.ACTIVE,
                ProductEventStatus.ENDED,
            ),
            statuses,
        )
    }

    @DisplayName("D11 할인율 금액은 원 미만을 버린다")
    @Test
    fun discountFloorsToWon() {
        // given
        val sale = rate(rate = 30)

        // when
        val price = sale.price(BigDecimal("10001.00"), BigDecimal.ZERO)

        // then
        assertEquals(BigDecimal("7000.00"), price)
    }

    @DisplayName("D12 이벤트 기본가 방식은 이벤트 기본가에 옵션 가격을 그대로 더한다")
    @Test
    fun fixedPriceAddsOptions() {
        // given
        val sale = fixed(BigDecimal("6000"))

        // when
        val price = sale.price(basePrice, BigDecimal("2500.00"))

        // then
        assertEquals(BigDecimal("8500.00"), price)
    }

    @DisplayName("상품의 이벤트 최저가와 총액은 각각 따로 버림하고, ACTIVE 가 아니면 비어 있고 정가에서 출발한다")
    @Test
    fun productEventPricesFollowStatus() {
        // given
        val product = productWithOptions("1001.00", "2001.00")
        product.changeEventSale(start, end, ProductEventPriceType.DISCOUNT_RATE, 50, null)

        // when
        val activeMin = product.eventMinPrice(start)
        val activeTotal = product.eventTotalPrice(start)
        val endedTotal = product.eventTotalPrice(end.plusDays(1))
        val endedStart = product.reservationStartPrice(end.plusDays(1))

        // then
        assertEquals(BigDecimal("5500.00"), activeMin)
        assertEquals(BigDecimal("6501.00"), activeTotal)
        assertNull(endedTotal)
        assertEquals(product.totalPrice, endedStart)
    }

    @DisplayName("이벤트를 지우면 상태가 NONE 이고 예약은 정가에서 출발한다")
    @Test
    fun clearEventSale() {
        // given
        val product = productWithOptions()
        product.changeEventSale(start, end, ProductEventPriceType.DISCOUNT_RATE, 30, null)

        // when
        product.clearEventSale()

        // then
        assertNull(product.eventSale)
        assertEquals(ProductEventStatus.NONE, product.eventStatus(start))
        assertEquals(product.totalPrice, product.reservationStartPrice(start))
    }

    private fun rate(
        startsOn: LocalDate? = start,
        endsOn: LocalDate? = end,
        rate: Int?,
    ): ProductEventSale = ProductEventSale.of(startsOn, endsOn, ProductEventPriceType.DISCOUNT_RATE, rate, null, basePrice)

    private fun fixed(value: BigDecimal?): ProductEventSale =
        ProductEventSale.of(start, end, ProductEventPriceType.FIXED_BASE_PRICE, null, value, basePrice)

    private fun productWithOptions(vararg optionPrices: String): Product {
        val product = Product(name = "상품", description = "설명", basePrice = basePrice)
        product.addOptions(
            optionPrices.mapIndexed { index, price ->
                Option(id = index + 1L, name = "옵션$index", description = "설명", price = BigDecimal(price), location = "서울")
            },
        )
        return product
    }

    companion object {
        private const val MISSING_MESSAGE = "이벤트를 켜면 시작일, 종료일, 가격 방식을 모두 입력해야 합니다."
        private const val RATE_MESSAGE = "할인율은 1 이상 99 이하의 정수여야 합니다."
        private const val MIXED_MESSAGE = "이벤트 가격 방식에 맞는 값 하나만 입력해야 합니다."
    }
}
