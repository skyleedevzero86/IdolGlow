package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.global.adapter.security.JwtProvider
import com.sleekydz86.idolglow.payment.application.AdminBillingStatsService
import com.sleekydz86.idolglow.payment.domain.RenewalDecision
import com.sleekydz86.idolglow.payment.domain.TossBillingRenewalNotice
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingRenewalNoticeJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDateTime
import java.util.UUID

@DisplayName("관리자 Glow Pass 구독 통계 API PostgreSQL")
class AdminBillingStatsApiPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var reservationFixture: PostgresReservationFixture

    @Autowired private lateinit var jwtProvider: JwtProvider

    @Autowired private lateinit var noticeRepository: TossBillingRenewalNoticeJpaRepository

    @Autowired private lateinit var service: AdminBillingStatsService

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    @DisplayName("S1·S9 비로그인은 401, 회원은 403, 관리자는 200 으로 통계를 받는다")
    @Test
    fun adminOnly() {
        // when
        val anonymous = mockMvc.perform(get(PATH))
        val member = mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(UserRole.USER)))
        val admin = mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, bearer(UserRole.ADMIN)))

        // then
        anonymous.andExpect(status().isUnauthorized)
        member.andExpect(status().isForbidden)
        admin
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.byCycle.length()").value(4))
            .andExpect(jsonPath("$.renewalNotice.sent").isNumber)
    }

    @DisplayName("S8 발송 시각이 정확히 7일 전과 조회 시각인 안내는 세고, 7일 전보다 1초 이르거나 1초 늦은 안내는 뺀다")
    @Test
    fun renewalWindowBoundaries() {
        // given
        val now = LocalDateTime.of(2032, 3, 10, 12, 0)
        saveNotice(now.minusDays(7), RenewalDecision.CONTINUE)
        saveNotice(now, RenewalDecision.STOP)
        saveNotice(now.minusDays(7).minusSeconds(1), RenewalDecision.CONTINUE)
        saveNotice(now.plusSeconds(1), null)

        // when
        val stats = service.stats(now)

        // then
        assertThat(stats.renewalNotice.sent).isEqualTo(2)
        assertThat(stats.renewalNotice.continued).isEqualTo(1)
        assertThat(stats.renewalNotice.stopped).isEqualTo(1)
        assertThat(stats.renewalNotice.noAnswer).isEqualTo(0)
    }

    private fun saveNotice(
        sentAt: LocalDateTime,
        decision: RenewalDecision?,
    ) {
        noticeRepository.save(
            TossBillingRenewalNotice(
                subscriptionId = 900_000L,
                userId = 900_000L,
                periodEnd = sentAt.plusDays(3),
                tokenHash = UUID.randomUUID().toString().replace("-", ""),
                sentAt = sentAt,
                decision = decision,
            ),
        )
    }

    private fun bearer(role: UserRole): String {
        val user = reservationFixture.persistUser()
        return "Bearer ${jwtProvider.generateToken(user.id, role).accessToken}"
    }

    private companion object {
        const val PATH = "/admin/billing/stats"
    }
}
