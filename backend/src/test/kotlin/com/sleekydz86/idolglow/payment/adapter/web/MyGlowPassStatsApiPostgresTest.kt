package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.global.adapter.security.JwtProvider
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@DisplayName("내 Glow Pass 이용 통계 API PostgreSQL")
class MyGlowPassStatsApiPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var reservationFixture: PostgresReservationFixture

    @Autowired private lateinit var jwtProvider: JwtProvider

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    @DisplayName("M1 비로그인은 401 이다")
    @Test
    fun anonymousRejected() {
        // when
        val result = mockMvc.perform(get(PATH))

        // then
        result.andExpect(status().isUnauthorized)
    }

    @DisplayName("M1·M4 본인 결제 완료 예약 할인만 더하고 다른 회원 예약은 넣지 않는다")
    @Test
    fun sumsOwnBookedDiscountOnly() {
        // given
        val me = reservationFixture.persistUser()
        val other = reservationFixture.persistUser()
        markBooked(reservationFixture.createReservation(me.id, reservationFixture.persistOpenSlot()).id, 1_200)
        markBooked(reservationFixture.createReservation(other.id, reservationFixture.persistOpenSlot()).id, 5_000)
        reservationFixture.createReservation(me.id, reservationFixture.persistOpenSlot())
        val token = jwtProvider.generateToken(me.id, UserRole.USER).accessToken

        // when
        val result = mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer $token"))

        // then
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.savedByDiscount").value(1_200))
            .andExpect(jsonPath("$.paidCount").value(0))
            .andExpect(jsonPath("$.membershipSince").doesNotExist())
    }

    private fun markBooked(
        reservationId: Long,
        discount: Int,
    ) {
        jdbcTemplate.update(
            "update reservations set status = 'BOOKED', membership_discount = ?, confirmed_at = now() where id = ?",
            discount,
            reservationId,
        )
        jdbcTemplate.update("update payments set status = 'SUCCEEDED' where reservation_id = ?", reservationId)
    }

    private companion object {
        const val PATH = "/payments/toss/billing/my-stats"
    }
}
