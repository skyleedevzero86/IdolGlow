package com.sleekydz86.idolglow.groundedsearch.domain

import com.sleekydz86.idolglow.productpackage.option.domain.Option
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductLocation
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("근거 문단 본문")
class GroundedChunkTextTest {
    @Test
    @DisplayName("문단 본문에 가격과 슬롯이 들어가지 않는다")
    fun chunkTextHasNoPriceOrSlot() {
        // given
        val product =
            Product(
                name = "홍대 메이크업",
                description = "체험 안내",
                basePrice = BigDecimal("88000"),
            )
        product.addTags(listOf("메이크업"))
        val day = LocalDate.now()
        product.addReservationSlots(day, day)
        product.setLocation(
            ProductLocation(
                product = product,
                name = "홍대점",
                latitude = BigDecimal("37.5511000"),
                longitude = BigDecimal("126.9237000"),
                roadAddressName = "서울특별시 마포구 홍익로",
                addressName = null,
                kakaoPlaceId = "kakao-hongdae",
            ),
        )
        val option = Option(name = "속눈썹", description = "연장", price = BigDecimal("15000"), location = "홍대점")

        // when
        val productText = GroundedChunkText.fromProduct(product)
        val optionText = GroundedChunkText.fromOption(option)

        // then
        assertTrue(productText.contains("홍대점"))
        assertTrue(productText.contains("메이크업"))
        assertTrue(productText.contains("홍익로"))
        assertFalse(productText.contains("88000"))
        assertFalse(productText.contains("09:00"))
        assertFalse(productText.contains("37.5511"))
        assertTrue(optionText.contains("속눈썹"))
        assertFalse(optionText.contains("15000"))
    }
}
