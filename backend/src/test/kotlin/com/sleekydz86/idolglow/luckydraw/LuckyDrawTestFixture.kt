package com.sleekydz86.idolglow.luckydraw

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.luckydraw.application.LuckyDrawClock
import com.sleekydz86.idolglow.luckydraw.application.LuckyDrawSeedSource
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDraw
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDrawDraft
import com.sleekydz86.idolglow.luckydraw.infrastructure.LuckyDrawJpaRepository
import com.sleekydz86.idolglow.productpackage.product.application.ProductEventCalendar
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductEventPriceType
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductCommandRepository
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.CreateReservationCommand
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Component
class LuckyDrawTestFixture(
    private val jdbcTemplate: JdbcTemplate,
    private val luckyDrawJpaRepository: LuckyDrawJpaRepository,
    private val luckyDrawClock: LuckyDrawClock,
    private val luckyDrawSeedSource: LuckyDrawSeedSource,
    private val productCommandRepository: ProductCommandRepository,
    private val productEventCalendar: ProductEventCalendar,
    private val reservationCommandService: ReservationCommandService,
) {
    fun clean() {
        listOf("lucky_draw_winners", "lucky_draw_tickets", "lucky_draw_entries", "lucky_draws").forEach { table ->
            jdbcTemplate.update("delete from $table")
        }
    }

    fun restore() {
        luckyDrawClock.clock = Clock.system(IdolGlowDateTimes.ZONE)
        luckyDrawSeedSource.nextSeedOverride = null
    }

    fun setNow(now: LocalDateTime) {
        luckyDrawClock.clock = Clock.fixed(now.atZone(IdolGlowDateTimes.ZONE).toInstant(), IdolGlowDateTimes.ZONE)
    }

    fun draw(
        startsAt: LocalDateTime,
        endsAt: LocalDateTime,
        drawAt: LocalDateTime = endsAt,
        winnerCount: Int = 1,
    ): LuckyDraw =
        luckyDrawJpaRepository.save(
            LuckyDraw.create(LuckyDrawDraft("응모", "경품", "설명", winnerCount, startsAt, endsAt, drawAt)),
        )

    fun entry(
        drawId: Long,
        userId: Long,
        date: LocalDate,
    ) {
        jdbcTemplate.update(
            "insert into lucky_draw_entries (lucky_draw_id, user_id, entry_date, entered_at, created_at, updated_at) " +
                "values (?, ?, ?, ?, now(), now())",
            drawId,
            userId,
            date,
            date.atTime(12, 0),
        )
    }

    fun paidReservation(
        userId: Long,
        approvedAt: LocalDateTime,
        status: String = "SUCCEEDED",
        event: Boolean = true,
        eventDays: ClosedRange<LocalDate>? = null,
    ): Long {
        val today = productEventCalendar.today()
        val range = eventDays ?: today.minusDays(30)..today.plusDays(30)
        val activeToday = event && today in range
        val visitDate = LocalDate.now().plusDays(3)
        val product =
            Product.createWithTimeSlots(
                name = "응모상품${UUID.randomUUID().toString().take(6)}",
                description = "설명",
                basePrice = BigDecimal("10000.00"),
                options = emptyList(),
                tagNames = emptyList(),
                slotStartDate = visitDate,
                slotEndDate = visitDate,
            )
        if (event) {
            product.changeEventSale(
                range.start,
                range.endInclusive,
                ProductEventPriceType.DISCOUNT_RATE,
                10,
                null,
            )
        }
        val saved = productCommandRepository.save(product)
        val created =
            reservationCommandService.createReservation(
                CreateReservationCommand(
                    productId = saved.id,
                    reservationSlotId = saved.reservationSlots.first().id,
                    userId = userId,
                    totalPrice = if (activeToday) BigDecimal("9000.00") else BigDecimal("10000.00"),
                ),
            )
        jdbcTemplate.update(
            "update payments set status = ?, approved_at = ? where reservation_id = ?",
            status,
            approvedAt,
            created.id,
        )
        return created.id
    }
}
