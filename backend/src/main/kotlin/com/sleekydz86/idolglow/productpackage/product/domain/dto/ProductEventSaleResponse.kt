package com.sleekydz86.idolglow.productpackage.product.domain.dto

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductEventPriceType
import com.sleekydz86.idolglow.productpackage.product.domain.ProductEventStatus
import com.sleekydz86.idolglow.productpackage.product.domain.eventMinPrice
import com.sleekydz86.idolglow.productpackage.product.domain.eventStatus
import com.sleekydz86.idolglow.productpackage.product.domain.eventTotalPrice
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

@Schema(description = "상품 이벤트(예약 판매) 정보. 이벤트를 끈 상품은 null")
data class ProductEventSaleResponse(
    @field:Schema(description = "이벤트 시작일(Asia/Seoul, 포함)", example = "2026-12-01")
    val startsOn: LocalDate,
    @field:Schema(description = "이벤트 종료일(Asia/Seoul, 포함)", example = "2026-12-31")
    val endsOn: LocalDate,
    @field:Schema(description = "가격 방식", example = "DISCOUNT_RATE")
    val priceType: ProductEventPriceType,
    @field:Schema(description = "할인율. DISCOUNT_RATE 일 때만", example = "30")
    val discountRate: Int?,
    @field:Schema(description = "이벤트 기본가. FIXED_BASE_PRICE 일 때만", example = "50000.00")
    val eventBasePrice: BigDecimal?,
    @field:Schema(description = "오늘(Asia/Seoul) 기준 이벤트 상태", example = "ACTIVE")
    val status: ProductEventStatus,
    @field:Schema(description = "이벤트 최저가. ACTIVE 가 아니면 null", example = "70000.00")
    val eventMinPrice: BigDecimal?,
    @field:Schema(description = "이벤트 총액(예약 금액의 출발 금액). ACTIVE 가 아니면 null", example = "210000.00")
    val eventTotalPrice: BigDecimal?,
) {
    companion object {
        fun of(
            product: Product,
            today: LocalDate,
        ): ProductEventSaleResponse? {
            val sale = product.eventSale ?: return null
            return ProductEventSaleResponse(
                startsOn = sale.startsOn,
                endsOn = sale.endsOn,
                priceType = sale.priceType,
                discountRate = sale.discountRate,
                eventBasePrice = sale.eventBasePrice,
                status = product.eventStatus(today),
                eventMinPrice = product.eventMinPrice(today),
                eventTotalPrice = product.eventTotalPrice(today),
            )
        }
    }
}
