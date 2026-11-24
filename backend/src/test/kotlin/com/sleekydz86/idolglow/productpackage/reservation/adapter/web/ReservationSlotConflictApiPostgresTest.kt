package com.sleekydz86.idolglow.productpackage.reservation.adapter.web

import com.sleekydz86.idolglow.global.adapter.security.JwtProvider
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDateTime

@DisplayName("예약 슬롯 충돌 응답 API")
class ReservationSlotConflictApiPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var fixture: PostgresReservationFixture

    @Autowired private lateinit var jwtProvider: JwtProvider

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @DisplayName("T9 GraphQL로 다른 예약이 선점 중인 슬롯을 예약하면 data 없이 오류 하나를 돌려준다")
    @Test
    fun graphQlRejectionForHeldSlot() {
        // given
        val slot = fixture.persistOpenSlot()
        fixture.createReservation(fixture.persistUser().id, slot)
        val latecomer = fixture.persistUser()
        val mutation =
            "mutation { createReservation(productId: \"${slot.productId}\", " +
                "input: { reservationSlotId: \"${slot.slotId}\", totalPrice: \"${slot.totalPrice}\" }) { id } }"

        // when
        val result =
            mockMvc.perform(
                post("/graphql")
                    .header(HttpHeaders.AUTHORIZATION, bearer(latecomer, UserRole.USER))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"query\":${quoted(mutation)}}"),
            )

        // then
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.errors.length()").value(1))
            .andExpect(jsonPath("$.errors[0].extensions.classification").value("INTERNAL_ERROR"))
    }

    @DisplayName("T1 예약이 확정된 슬롯을 예약하면 409와 RESERVATION_SLOT_ALREADY_BOOKED를 돌려주고 예약·결제를 만들지 않는다")
    @Test
    fun rejectsBookedSlot() {
        // given
        val slot = fixture.persistOpenSlot()
        markBooked(slot)
        val before = rowCounts(slot)

        // when
        val result = reserve(fixture.persistUser(), slot)

        // then
        result
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("RESERVATION_SLOT_ALREADY_BOOKED"))
            .andExpect(jsonPath("$.message").value("이미 예약된 슬롯입니다."))
        assertEquals(before, rowCounts(slot))
        assertEquals(SlotState(booked = true, holdReservationId = null), slotState(slot))
    }

    @DisplayName("T2 다른 예약이 선점 중인 슬롯을 예약하면 409와 RESERVATION_SLOT_HELD를 돌려주고 슬롯 선점을 바꾸지 않는다")
    @Test
    fun rejectsHeldSlot() {
        // given
        val slot = fixture.persistOpenSlot()
        val holder = fixture.createReservation(fixture.persistUser().id, slot)
        val before = rowCounts(slot)
        val heldBefore = holdExpiresAt(slot)

        // when
        val result = reserve(fixture.persistUser(), slot)

        // then
        result
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("RESERVATION_SLOT_HELD"))
            .andExpect(jsonPath("$.message").value("다른 예약이 이미 선점한 슬롯입니다."))
        assertEquals(before, rowCounts(slot))
        assertEquals(SlotState(booked = false, holdReservationId = holder.id), slotState(slot))
        assertEquals(heldBefore, holdExpiresAt(slot))
    }

    @DisplayName("T3 선점 만료 시각이 지난 슬롯은 새 예약을 받고 선점을 새 예약으로 바꾼다")
    @Test
    fun acceptsSlotWithExpiredHold() {
        // given
        val slot = fixture.persistOpenSlot()
        fixture.createReservation(fixture.persistUser().id, slot)
        jdbcTemplate.update(
            "update reservation_slots set hold_expires_at = ? where id = ?",
            LocalDateTime.now().minusMinutes(1),
            slot.slotId,
        )

        // when
        val result = reserve(fixture.persistUser(), slot)

        // then
        val created =
            result
                .andExpect(status().isCreated)
                .andReturn()
                .response.contentAsString
        val newId = Regex("\"id\":(\\d+)").find(created)!!.groupValues[1].toLong()
        assertEquals(SlotState(booked = false, holdReservationId = newId), slotState(slot))
    }

    @DisplayName("T4 요청한 상품에 속하지 않는 슬롯이면 400과 BAD_REQUEST를 돌려준다")
    @Test
    fun rejectsSlotOfOtherProduct() {
        // given
        val slot = fixture.persistOpenSlot()
        val other = fixture.persistOpenSlot()

        // when
        val result = reserve(fixture.persistUser(), slot.copy(productId = other.productId))

        // then
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
    }

    @DisplayName("T5 관리자가 예약 확정 슬롯을 지우면 409와 RESERVATION_SLOT_ALREADY_BOOKED를 돌려주고 슬롯을 남긴다")
    @Test
    fun adminCannotDeleteBookedSlot() {
        // given
        val slot = fixture.persistOpenSlot()
        markBooked(slot)

        // when
        val result = deleteSlot(slot, fixture.persistUser(), UserRole.ADMIN)

        // then
        result
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("RESERVATION_SLOT_ALREADY_BOOKED"))
        assertEquals(1, slotRows(slot))
    }

    @DisplayName("T5 관리자가 선점 중 슬롯을 지우면 409와 RESERVATION_SLOT_HELD를 돌려주고 슬롯을 남긴다")
    @Test
    fun adminCannotDeleteHeldSlot() {
        // given
        val slot = fixture.persistOpenSlot()
        fixture.createReservation(fixture.persistUser().id, slot)

        // when
        val result = deleteSlot(slot, fixture.persistUser(), UserRole.ADMIN)

        // then
        result
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("RESERVATION_SLOT_HELD"))
        assertEquals(1, slotRows(slot))
    }

    @DisplayName("T5 일반 회원은 슬롯 삭제에서 403을 받고 슬롯은 남는다")
    @Test
    fun memberCannotDeleteSlot() {
        // given
        val slot = fixture.persistOpenSlot()
        markBooked(slot)

        // when
        val result = deleteSlot(slot, fixture.persistUser(), UserRole.USER)

        // then
        result.andExpect(status().isForbidden)
        assertEquals(1, slotRows(slot))
    }

    private fun reserve(
        user: User,
        slot: PostgresReservationFixture.OpenSlot,
    ): ResultActions =
        mockMvc.perform(
            post("/products/${slot.productId}/reservations")
                .header(HttpHeaders.AUTHORIZATION, bearer(user, UserRole.USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reservationSlotId\":${slot.slotId},\"totalPrice\":${slot.totalPrice}}"),
        )

    private fun deleteSlot(
        slot: PostgresReservationFixture.OpenSlot,
        user: User,
        role: UserRole,
    ): ResultActions =
        mockMvc.perform(
            delete("/admin/slots/${slot.slotId}")
                .header(HttpHeaders.AUTHORIZATION, bearer(user, role)),
        )

    private fun markBooked(slot: PostgresReservationFixture.OpenSlot) {
        jdbcTemplate.update(
            "update reservation_slots set is_booked = true, hold_reservation_id = null, hold_expires_at = null " +
                "where id = ?",
            slot.slotId,
        )
    }

    private fun rowCounts(slot: PostgresReservationFixture.OpenSlot): Pair<Long, Long> {
        val reservations =
            jdbcTemplate.queryForObject(
                "select count(*) from reservations where reservation_slot_id = ?",
                Long::class.java,
                slot.slotId,
            )!!
        val payments =
            jdbcTemplate.queryForObject(
                "select count(*) from payments p join reservations r on r.id = p.reservation_id " +
                    "where r.reservation_slot_id = ?",
                Long::class.java,
                slot.slotId,
            )!!
        return reservations to payments
    }

    private fun slotState(slot: PostgresReservationFixture.OpenSlot): SlotState =
        jdbcTemplate.queryForObject(
            "select is_booked, hold_reservation_id from reservation_slots where id = ?",
            { rs, _ -> SlotState(rs.getBoolean(1), rs.getObject(2)?.let { (it as Number).toLong() }) },
            slot.slotId,
        )!!

    private fun holdExpiresAt(slot: PostgresReservationFixture.OpenSlot): LocalDateTime? =
        jdbcTemplate.queryForObject(
            "select hold_expires_at from reservation_slots where id = ?",
            LocalDateTime::class.java,
            slot.slotId,
        )

    private fun slotRows(slot: PostgresReservationFixture.OpenSlot): Int =
        jdbcTemplate.queryForObject(
            "select count(*) from reservation_slots where id = ?",
            Int::class.java,
            slot.slotId,
        )!!

    private data class SlotState(
        val booked: Boolean,
        val holdReservationId: Long?,
    )

    private fun bearer(
        user: User,
        role: UserRole,
    ): String = "Bearer ${jwtProvider.generateToken(user.id, role).accessToken}"

    private fun quoted(text: String): String = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
