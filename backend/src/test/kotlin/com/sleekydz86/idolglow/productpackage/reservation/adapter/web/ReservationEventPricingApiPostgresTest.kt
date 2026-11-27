package com.sleekydz86.idolglow.productpackage.reservation.adapter.web

import com.sleekydz86.idolglow.global.adapter.security.JwtProvider
import com.sleekydz86.idolglow.productpackage.product.application.ProductEventCalendar
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductEventPriceType
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductCommandRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@DisplayName("이벤트 상품 예약 금액 REST·GraphQL")
class ReservationEventPricingApiPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var fixture: PostgresReservationFixture

    @Autowired private lateinit var jwtProvider: JwtProvider

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var productCommandRepository: ProductCommandRepository

    @Autowired private lateinit var productEventCalendar: ProductEventCalendar

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @DisplayName("REST 로 진행 중 이벤트 상품에 정가를 보내면 400 BAD_REQUEST 이고 예약·결제를 만들지 않는다")
    @Test
    fun restRejectsRegularPrice() {
        // given
        val slot = persistActiveEventSlot()

        // when
        val result = reserveRest(fixture.persistUser(), slot, "10000.00")

        // then
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
        assertEquals(0 to 0, rowCounts(slot))
    }

    @DisplayName("REST 로 진행 중 이벤트 상품에 이벤트 총액을 보내면 201 이고 그 금액으로 저장한다")
    @Test
    fun restAcceptsEventPrice() {
        // given
        val slot = persistActiveEventSlot()

        // when
        val result = reserveRest(fixture.persistUser(), slot, "7000.00")

        // then
        result.andExpect(status().isCreated)
        assertEquals(1 to 1, rowCounts(slot))
        assertEquals(BigDecimal("7000.00"), storedTotal(slot))
    }

    @DisplayName("GraphQL 로 진행 중 이벤트 상품에 정가를 보내면 data 없이 오류 하나이고 예약·결제를 만들지 않는다")
    @Test
    fun graphQlRejectsRegularPrice() {
        // given
        val slot = persistActiveEventSlot()

        // when
        val result = reserveGraphQl(fixture.persistUser(), slot, "10000.00")

        // then
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.errors.length()").value(1))
        assertEquals(0 to 0, rowCounts(slot))
    }

    @DisplayName("GraphQL 로 진행 중 이벤트 상품에 이벤트 총액을 보내면 예약을 만들고 그 금액으로 저장한다")
    @Test
    fun graphQlAcceptsEventPrice() {
        // given
        val slot = persistActiveEventSlot()

        // when
        val result = reserveGraphQl(fixture.persistUser(), slot, "7000.00")

        // then
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.data.createReservation.id").exists())
        assertEquals(1 to 1, rowCounts(slot))
        assertEquals(BigDecimal("7000.00"), storedTotal(slot))
    }

    private fun persistActiveEventSlot(): PostgresReservationFixture.OpenSlot {
        val today: LocalDate = productEventCalendar.today()
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
        product.changeEventSale(today.minusDays(1), today.plusDays(1), ProductEventPriceType.DISCOUNT_RATE, 30, null)
        val saved = productCommandRepository.save(product)
        return PostgresReservationFixture.OpenSlot(saved.id, saved.reservationSlots.first().id, saved.totalPrice)
    }

    private fun reserveRest(
        user: User,
        slot: PostgresReservationFixture.OpenSlot,
        totalPrice: String,
    ): ResultActions =
        mockMvc.perform(
            post("/products/${slot.productId}/reservations")
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reservationSlotId\":${slot.slotId},\"totalPrice\":$totalPrice}"),
        )

    private fun reserveGraphQl(
        user: User,
        slot: PostgresReservationFixture.OpenSlot,
        totalPrice: String,
    ): ResultActions {
        val mutation =
            "mutation { createReservation(productId: \"${slot.productId}\", " +
                "input: { reservationSlotId: \"${slot.slotId}\", totalPrice: \"$totalPrice\" }) { id } }"
        return mockMvc.perform(
            post("/graphql")
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":${quoted(mutation)}}"),
        )
    }

    private fun rowCounts(slot: PostgresReservationFixture.OpenSlot): Pair<Int, Int> {
        val reservations =
            jdbcTemplate.queryForObject(
                "select count(*) from reservations where reservation_slot_id = ?",
                Int::class.java,
                slot.slotId,
            )!!
        val payments =
            jdbcTemplate.queryForObject(
                "select count(*) from payments p join reservations r on r.id = p.reservation_id " +
                    "where r.reservation_slot_id = ?",
                Int::class.java,
                slot.slotId,
            )!!
        return reservations to payments
    }

    private fun storedTotal(slot: PostgresReservationFixture.OpenSlot): BigDecimal =
        jdbcTemplate.queryForObject(
            "select total_price from reservations where reservation_slot_id = ?",
            BigDecimal::class.java,
            slot.slotId,
        )!!

    private fun bearer(user: User): String = "Bearer ${jwtProvider.generateToken(user.id, UserRole.USER).accessToken}"

    private fun quoted(text: String): String = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
