package com.sleekydz86.idolglow.productpackage.product.adapter.web.request

import com.sleekydz86.idolglow.productpackage.product.application.dto.ProductEventSaleInput
import com.sleekydz86.idolglow.productpackage.product.domain.ProductEventPriceType
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

@Schema(description = "상품 이벤트(예약 판매) 설정. 보내지 않거나 null 이면 이벤트를 끈다")
data class ProductEventSaleRequest(
    @field:Schema(description = "이벤트 시작일(Asia/Seoul, 포함)", example = "2026-12-01")
    val startsOn: LocalDate? = null,
    @field:Schema(description = "이벤트 종료일(Asia/Seoul, 포함). 시작일과 같거나 뒤", example = "2026-12-31")
    val endsOn: LocalDate? = null,
    @field:Schema(description = "가격 방식", example = "DISCOUNT_RATE")
    val priceType: ProductEventPriceType? = null,
    @field:Schema(description = "할인율(1~99 정수). DISCOUNT_RATE 일 때만", example = "30")
    val discountRate: Int? = null,
    @field:Schema(description = "이벤트 기본가(원, 0 이상, 상품 기본가 미만). FIXED_BASE_PRICE 일 때만", example = "50000")
    val eventBasePrice: BigDecimal? = null,
) {
    fun toInput(): ProductEventSaleInput =
        ProductEventSaleInput(
            startsOn = startsOn,
            endsOn = endsOn,
            priceType = priceType,
            discountRate = discountRate,
            eventBasePrice = eventBasePrice,
        )
}
