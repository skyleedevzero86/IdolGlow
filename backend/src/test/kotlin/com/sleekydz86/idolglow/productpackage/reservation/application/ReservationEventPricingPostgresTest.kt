package com.sleekydz86.idolglow.productpackage.reservation.application

import com.sleekydz86.idolglow.payment.application.BillingDefaults
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import com.sleekydz86.idolglow.productpackage.product.application.ProductEventCalendar
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductEventPriceType
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductCommandRepository
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.CreateReservationCommand
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@DisplayName("이벤트 상품 예약 금액 PostgreSQL")
class ReservationEventPricingPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var fixture: PostgresReservationFixture

    @Autowired private lateinit var reservationCommandService: ReservationCommandService

    @Autowired private lateinit var productCommandRepository: ProductCommandRepository

    @Autowired private lateinit var productEventCalendar: ProductEventCalendar

    @Autowired private lateinit var methodRepository: TossBillingMethodJpaRepository

    @Autowired private lateinit var subscriptionRepository: TossBillingSubscriptionJpaRepository

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var transactionManager: PlatformTransactionManager

    @DisplayName("진행 중 이벤트는 비회원 예약 금액과 결제 금액을 이벤트 총액으로 저장한다")
    @Test
    fun activeEventChargesEventPriceForNonMember() {
        // given
        val slot = persistEventSlot(today().minusDays(1), today().plusDays(1))
        val userId = fixture.persistUser().id

        // when
        val created = reserve(userId, slot, BigDecimal("7000.00"))

        // then
        assertEquals(BigDecimal("7000.00"), reservationTotal(created.id))
        assertEquals(BigDecimal("0.00"), membershipDiscount(created.id))
        assertEquals(BigDecimal("7000.00"), paymentAmount(created.id))
    }

    @DisplayName("진행 중 이벤트는 회원에게 이벤트 총액 뒤 멤버십 할인을 겹쳐 적용하고 멤버십 할인만 기록한다")
    @Test
    fun activeEventStacksMembershipDiscount() {
        // given
        val slot = persistEventSlot(today(), today())
        val userId = persistMember()

        // when
        val created = reserve(userId, slot, BigDecimal("6300.00"))

        // then
        assertEquals(BigDecimal("6300.00"), reservationTotal(created.id))
        assertEquals(BigDecimal("700.00"), membershipDiscount(created.id))
        assertEquals(BigDecimal("6300.00"), paymentAmount(created.id))
    }

    @DisplayName("진행 중 이벤트에 정가를 보내면 400 사유로 거절하고 예약·결제·슬롯 선점을 남기지 않는다")
    @Test
    fun activeEventRejectsRegularPrice() {
        // given
        val slot = persistEventSlot(today().minusDays(1), today().plusDays(1))
        val userId = fixture.persistUser().id

        // when
        val error = assertThrows<IllegalArgumentException> { reserve(userId, slot, BigDecimal("10000.00")) }

        // then
        assertEquals(true, error.message?.startsWith("상품 ${slot.productId}의 총금액이 일치하지 않습니다."))
        assertEquals(0, countReservations(slot.slotId))
        assertEquals(0, countPayments(slot.slotId))
        assertNull(holdReservationId(slot.slotId))
    }

    @DisplayName("끝난 이벤트는 이벤트 금액을 거절하고 정가는 받는다")
    @Test
    fun endedEventUsesRegularPrice() {
        // given
        val slot = persistEventSlot(today().minusDays(3), today().minusDays(1))
        val userId = fixture.persistUser().id

        // when
        val error = assertThrows<IllegalArgumentException> { reserve(userId, slot, BigDecimal("7000.00")) }
        val created = reserve(userId, slot, BigDecimal("10000.00"))

        // then
        assertEquals(true, error.message?.startsWith("상품 ${slot.productId}의 총금액이 일치하지 않습니다."))
        assertEquals(BigDecimal("10000.00"), reservationTotal(created.id))
        assertEquals(BigDecimal("10000.00"), paymentAmount(created.id))
    }

    @DisplayName("시작 전 이벤트는 정가를 받는다")
    @Test
    fun scheduledEventUsesRegularPrice() {
        // given
        val slot = persistEventSlot(today().plusDays(1), today().plusDays(2))
        val userId = fixture.persistUser().id

        // when
        val created = reserve(userId, slot, BigDecimal("10000.00"))

        // then
        assertEquals(BigDecimal("10000.00"), reservationTotal(created.id))
    }

    @DisplayName("예약 뒤 관리자가 이벤트를 꺼도 이미 만든 예약·결제 금액은 바뀌지 않는다")
    @Test
    fun existingReservationKeepsPriceAfterEventCleared() {
        // given
        val slot = persistEventSlot(today().minusDays(1), today().plusDays(1))
        val created = reserve(fixture.persistUser().id, slot, BigDecimal("7000.00"))

        // when
        transaction {
            val product = requireNotNull(productCommandRepository.findById(slot.productId))
            product.clearEventSale()
            productCommandRepository.save(product)
        }

        // then
        assertEquals(BigDecimal("7000.00"), reservationTotal(created.id))
        assertEquals(BigDecimal("7000.00"), paymentAmount(created.id))
    }

    private fun today(): LocalDate = productEventCalendar.today()

    private fun persistEventSlot(
        startsOn: LocalDate,
        endsOn: LocalDate,
    ): PostgresReservationFixture.OpenSlot {
        val visitDate = LocalDate.now().plusDays(3)
        val product =
            Product.createWithTimeSlots(
                name = "이벤트${UUID.randomUUID().toString().take(8)}",
                description = "설명",
                basePrice = BigDecimal("10000.00"),
                options = emptyList(),
                tagNames = emptyList(),
                slotStartDate = visitDate,
                slotEndDate = visitDate,
            )
        product.changeEventSale(startsOn, endsOn, ProductEventPriceType.DISCOUNT_RATE, 30, null)
        val saved = productCommandRepository.save(product)
        return PostgresReservationFixture.OpenSlot(saved.id, saved.reservationSlots.first().id, saved.totalPrice)
    }

    private fun persistMember(): Long {
        val user = fixture.persistUser()
        val method =
            methodRepository.save(
                TossBillingMethod(
                    userId = user.id,
                    customerKey = "idolglow-user-${user.id}",
                    billingKey = "bk-${user.id}",
                    isDefault = true,
                ),
            )
        subscriptionRepository.save(
            TossBillingSubscription(
                userId = user.id,
                billingMethod = method,
                orderName = BillingDefaults.DEFAULT_ORDER_NAME,
                amount = 9900L,
                status = TossBillingSubscriptionStatus.ACTIVE,
                cycleMonths = 1,
            ),
        )
        return user.id
    }

    private fun reserve(
        userId: Long,
        slot: PostgresReservationFixture.OpenSlot,
        totalPrice: BigDecimal,
    ) = reservationCommandService.createReservation(
        CreateReservationCommand(
            productId = slot.productId,
            reservationSlotId = slot.slotId,
            userId = userId,
            totalPrice = totalPrice,
        ),
    )

    private fun reservationTotal(reservationId: Long): BigDecimal =
        jdbcTemplate.queryForObject("select total_price from reservations where id = ?", BigDecimal::class.java, reservationId)!!

    private fun membershipDiscount(reservationId: Long): BigDecimal =
        jdbcTemplate.queryForObject(
            "select membership_discount from reservations where id = ?",
            BigDecimal::class.java,
            reservationId,
        )!!

    private fun paymentAmount(reservationId: Long): BigDecimal =
        jdbcTemplate.queryForObject("select amount from payments where reservation_id = ?", BigDecimal::class.java, reservationId)!!

    private fun countReservations(slotId: Long): Int =
        jdbcTemplate.queryForObject("select count(*) from reservations where reservation_slot_id = ?", Int::class.java, slotId)!!

    private fun countPayments(slotId: Long): Int =
        jdbcTemplate.queryForObject(
            "select count(*) from payments p join reservations r on r.id = p.reservation_id where r.reservation_slot_id = ?",
            Int::class.java,
            slotId,
        )!!

    private fun holdReservationId(slotId: Long): Long? =
        jdbcTemplate.queryForObject("select hold_reservation_id from reservation_slots where id = ?", Long::class.javaObjectType, slotId)

    private fun transaction(block: () -> Unit) {
        TransactionTemplate(transactionManager).executeWithoutResult { block() }
    }
}
