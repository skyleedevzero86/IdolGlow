package com.sleekydz86.idolglow.groundedsearch.application

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedContentHash
import com.sleekydz86.idolglow.groundedsearch.domain.RagChunk
import com.sleekydz86.idolglow.groundedsearch.domain.RagSourceType
import com.sleekydz86.idolglow.groundedsearch.infrastructure.RagChunkJpaRepository
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductJpaRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import com.sleekydz86.idolglow.productpackage.reservation.infrastructure.ReservationSlotJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@DisplayName("근거 검색 예약 가능 슬롯")
class GroundedAnswerPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var answerService: GroundedAnswerService

    @Autowired
    private lateinit var productRepository: ProductJpaRepository

    @Autowired
    private lateinit var slotRepository: ReservationSlotJpaRepository

    @Autowired
    private lateinit var chunkRepository: RagChunkJpaRepository

    @Test
    @DisplayName("예약된 슬롯은 가능한 시간으로 나오지 않는다")
    fun soldOutSlotNeverShownAsAvailable() {

        // given
        jdbcTemplate.execute("create extension if not exists pg_trgm")
        val product = productRepository.save(Product(name = "매진 확인 상품", description = "설명", basePrice = BigDecimal.ONE))
        val openStart = LocalTime.of((2 shl 2) + 1, 0)
        val bookedStart = openStart.plusHours(1)
        val day = LocalDate.now()
        val now = LocalDateTime.now()
        saveBooked(product, day, bookedStart, now)
        slotRepository.saveAndFlush(
            ReservationSlot(
                product = product,
                reservationDate = day,
                startTime = openStart,
                endTime = openStart.plusHours(1),
            ),
        )
        val query = "매진확인전용홍대"
        chunkRepository.saveAndFlush(
            RagChunk(
                sourceType = RagSourceType.PRODUCT,
                sourceId = product.id,
                linkKey = "product/${product.id}",
                content = query,
                contentHash = GroundedContentHash.of(query),
            ),
        )

        // when
        val answer = answerService.answer(query)

        // then
        val slots = answer.products.first { it.productId == product.id }.slots
        assertTrue(slots.none { it.start == bookedStart })
        assertTrue(slots.any { it.start == openStart })
    }

    private fun saveBooked(product: Product, day: LocalDate, start: LocalTime, now: LocalDateTime) {
        val slot = ReservationSlot(
            product = product,
            reservationDate = day,
            startTime = start,
            endTime = start.plusHours(1),
        )
        slot.hold(1L, now.plusHours(2), now)
        slot.confirmBooking(1L, now)
        slotRepository.saveAndFlush(slot)
    }
}
