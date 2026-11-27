package com.sleekydz86.idolglow.productpackage.product.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Embeddable
class ProductEventSale(
    @Column(name = "event_starts_on")
    val startsOn: LocalDate,
    @Column(name = "event_ends_on")
    val endsOn: LocalDate,
    @Enumerated(EnumType.STRING)
    @Column(name = "event_price_type", length = 20)
    val priceType: ProductEventPriceType,
    @Column(name = "event_discount_rate")
    val discountRate: Int? = null,
    @Column(name = "event_base_price", precision = 19, scale = 2)
    val eventBasePrice: BigDecimal? = null,
) {
    fun status(today: LocalDate): ProductEventStatus =
        when {
            today.isBefore(startsOn) -> ProductEventStatus.SCHEDULED
            today.isAfter(endsOn) -> ProductEventStatus.ENDED
            else -> ProductEventStatus.ACTIVE
        }

    fun price(
        regularBasePrice: BigDecimal,
        optionsPrice: BigDecimal,
    ): BigDecimal =
        when (priceType) {
            ProductEventPriceType.DISCOUNT_RATE -> discounted(regularBasePrice + optionsPrice)
            ProductEventPriceType.FIXED_BASE_PRICE ->
                requireNotNull(eventBasePrice).setScale(MONEY_SCALE)
        }

    private fun discounted(amount: BigDecimal): BigDecimal =
        amount
            .multiply(BigDecimal(PERCENT - requireNotNull(discountRate)))
            .divide(BigDecimal(PERCENT), 0, RoundingMode.FLOOR)
            .setScale(MONEY_SCALE)

    companion object {
        const val MIN_DISCOUNT_RATE = 1
        const val MAX_DISCOUNT_RATE = 99
        private const val PERCENT = 100
        private const val MONEY_SCALE = 2

        fun of(
            startsOn: LocalDate?,
            endsOn: LocalDate?,
            priceType: ProductEventPriceType?,
            discountRate: Int?,
            eventBasePrice: BigDecimal?,
            productBasePrice: BigDecimal,
        ): ProductEventSale {
            require(startsOn != null && endsOn != null && priceType != null) {
                "이벤트를 켜면 시작일, 종료일, 가격 방식을 모두 입력해야 합니다."
            }
            require(!endsOn.isBefore(startsOn)) { "이벤트 종료일은 시작일과 같거나 뒤여야 합니다." }
            return when (priceType) {
                ProductEventPriceType.DISCOUNT_RATE -> discountRateSale(startsOn, endsOn, discountRate, eventBasePrice)
                ProductEventPriceType.FIXED_BASE_PRICE ->
                    fixedBasePriceSale(startsOn, endsOn, discountRate, eventBasePrice, productBasePrice)
            }
        }

        private fun discountRateSale(
            startsOn: LocalDate,
            endsOn: LocalDate,
            discountRate: Int?,
            eventBasePrice: BigDecimal?,
        ): ProductEventSale {
            require(eventBasePrice == null) { MIXED_VALUES_MESSAGE }
            require(discountRate != null && discountRate in MIN_DISCOUNT_RATE..MAX_DISCOUNT_RATE) {
                "할인율은 1 이상 99 이하의 정수여야 합니다."
            }
            return ProductEventSale(startsOn, endsOn, ProductEventPriceType.DISCOUNT_RATE, discountRate = discountRate)
        }

        private fun fixedBasePriceSale(
            startsOn: LocalDate,
            endsOn: LocalDate,
            discountRate: Int?,
            eventBasePrice: BigDecimal?,
            productBasePrice: BigDecimal,
        ): ProductEventSale {
            require(discountRate == null) { MIXED_VALUES_MESSAGE }
            require(productBasePrice.signum() > 0) { "상품 기본가가 0원이면 이벤트 기본가 방식을 쓸 수 없습니다." }
            require(eventBasePrice != null && eventBasePrice.signum() >= 0 && eventBasePrice < productBasePrice) {
                "이벤트 기본가는 0원 이상이고 상품 기본가보다 작아야 합니다."
            }
            require(eventBasePrice.stripTrailingZeros().scale() <= 0) { "이벤트 기본가는 원 단위 정수여야 합니다." }
            return ProductEventSale(
                startsOn,
                endsOn,
                ProductEventPriceType.FIXED_BASE_PRICE,
                eventBasePrice = eventBasePrice.setScale(MONEY_SCALE),
            )
        }

        private const val MIXED_VALUES_MESSAGE = "이벤트 가격 방식에 맞는 값 하나만 입력해야 합니다."
    }
}
