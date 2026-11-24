package com.sleekydz86.idolglow.admin.adapter.web

import com.sleekydz86.idolglow.global.support.DashboardTime
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.payment.application.BillingDefaults
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingMethodJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.visit.application.SiteVisitTxService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@DisplayName("관리자 대시보드 API")
class AdminDashboardApiPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var dashboardTime: DashboardTime

    @Autowired private lateinit var fixture: PostgresReservationFixture

    @Autowired private lateinit var reservationRepository: ReservationRepository

    @Autowired private lateinit var paymentRepository: PaymentRepository

    @Autowired private lateinit var methodRepository: TossBillingMethodJpaRepository

    @Autowired private lateinit var subscriptionRepository: TossBillingSubscriptionJpaRepository

    @Autowired private lateinit var siteVisitTxService: SiteVisitTxService

    @Autowired private lateinit var objectMapper: ObjectMapper

    private lateinit var mockMvc: MockMvc

    private val today: LocalDate = LocalDate.of(2031, 3, 15)

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
        dashboardTime.clock =
            Clock.fixed(today.atTime(10, 0).atZone(IdolGlowDateTimes.ZONE).toInstant(), IdolGlowDateTimes.ZONE)
        dashboardTime.storageZone = IdolGlowDateTimes.ZONE
    }

    @AfterEach
    fun restore() {
        dashboardTime.clock = Clock.system(IdolGlowDateTimes.ZONE)
        dashboardTime.storageZone = ZoneId.systemDefault()
    }

    @DisplayName("오늘·최근 7일 신규 가입은 서울 날짜로 세고, 가입 시각 없는 회원은 빼며, 전체 회원 수는 모두 센다")
    @Test
    fun countsJoinedMembersWithinSeoulDays() {
        // given
        joinedAt(fixture.persistUser(), today.atTime(0, 0))
        joinedAt(fixture.persistUser(), today.atTime(23, 59, 59))
        joinedAt(fixture.persistUser(), today.minusDays(6).atStartOfDay())
        joinedAt(fixture.persistUser(), today.minusDays(7).atTime(23, 59, 59))
        joinedAt(fixture.persistUser(), today.plusDays(1).atStartOfDay())
        joinedAt(fixture.persistUser(), null)

        // when
        val body = summary()

        // then
        assertEquals(2L, body["members"]["joinedToday"].asLong())
        assertEquals(3L, body["members"]["joinedLast7Days"].asLong())
        assertEquals(countUsers(), body["members"]["total"].asLong())
    }

    @DisplayName("방문 추이는 오늘 포함 7일을 오래된 날부터 주고, 기록 없는 날은 0이며, 신규 방문자는 첫 방문일로 센다")
    @Test
    fun summarizesVisitorsForSevenDays() {
        // given
        val returning = UUID.randomUUID().toString()
        siteVisitTxService.touchVisitor(returning, today.minusDays(10))
        visit(returning, today.minusDays(2))
        visit(returning, today)
        val firstToday = UUID.randomUUID().toString()
        visit(firstToday, today)
        visit(UUID.randomUUID().toString(), today.minusDays(6))
        visit(UUID.randomUUID().toString(), today.minusDays(7))

        // when
        val daily = summary()["visitors"]

        // then
        assertEquals(2L, daily["today"].asLong())
        assertEquals(1L, daily["newToday"].asLong())
        val days = elements(daily["daily"])
        assertEquals((6L downTo 0L).map { today.minusDays(it).toString() }, days.map { it["date"].asString() })
        assertEquals(listOf(1L, 0L, 0L, 0L, 1L, 0L, 2L), days.map { it["visitors"].asLong() })
        assertEquals(listOf(1L, 0L, 0L, 0L, 0L, 0L, 1L), days.map { it["newVisitors"].asLong() })
    }

    @DisplayName("이번 달 결제 순액은 서울 1일 0시부터 승인된 금액에서 취소 금액을 빼고, 지난달 승인·미승인은 넣지 않는다")
    @Test
    fun sumsThisMonthNetApprovedAmount() {
        // given
        payment(BigDecimal("30000"), today.withDayOfMonth(1).atStartOfDay(), BigDecimal("12500.50"))
        payment(BigDecimal("20000"), today.atTime(9, 0), null)
        payment(BigDecimal("50000"), today.withDayOfMonth(1).minusDays(1).atTime(23, 59, 59), null)
        payment(BigDecimal("70000"), null, null)

        // when
        val revenue = summary()["revenue"]

        // then
        assertEquals(37499L, revenue["thisMonthNetAmount"].asLong())
    }

    @DisplayName("정기결제 월 환산은 ACTIVE 만 넣고 주기 개월 수로 나눠 원 단위 아래를 버리며, 월 매출 예상은 두 금액의 합이다")
    @Test
    fun sumsActiveBillingMonthlyAmount() {
        // given
        val before = summary()["revenue"]
        billing(13900, 3, TossBillingSubscriptionStatus.ACTIVE)
        billing(4900, 1, TossBillingSubscriptionStatus.ACTIVE)
        billing(47000, 12, TossBillingSubscriptionStatus.CANCEL_SCHEDULED)
        billing(25900, 6, TossBillingSubscriptionStatus.PAUSED)

        // when
        val after = summary()["revenue"]

        // then
        val monthlyDelta = after["billingMonthlyAmount"].asLong() - before["billingMonthlyAmount"].asLong()
        assertEquals(4633L + 4900L, monthlyDelta)
        assertEquals(2L, after["activeBillingCount"].asLong() - before["activeBillingCount"].asLong())
        assertEquals(
            after["thisMonthNetAmount"].asLong() + after["billingMonthlyAmount"].asLong(),
            after["expectedMonthlyAmount"].asLong(),
        )
    }

    @DisplayName("최근 가입 회원은 가입 시각 최신 순이고 같으면 번호 큰 순이며, 가입 시각 없는 회원과 이메일은 넣지 않는다")
    @Test
    fun listsRecentMembersWithoutEmail() {
        // given
        val sameTime = LocalDateTime.of(2050, 1, 1, 0, 0)
        val older = fixture.persistUser().also { joinedAt(it, sameTime.minusDays(1)) }
        val first = fixture.persistUser().also { joinedAt(it, sameTime) }
        val second = fixture.persistUser().also { joinedAt(it, sameTime) }

        // when
        val result =
            mockMvc
                .perform(admin(get("/admin/dashboard/recent-members").param("limit", "3")))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andReturn()

        // then
        val ids = elements(objectMapper.readTree(result.response.contentAsString)).map { it["userId"].asLong() }
        assertEquals(listOf(second.id, first.id, older.id), ids)
    }

    @DisplayName("최근 가입 회원 개수는 1~20으로 맞추고, 없거나 숫자가 아니면 8이다")
    @ParameterizedTest
    @CsvSource("0,1", "-5,1", "21,20", "abc,8", "'',8", "' 3 ',3")
    fun clampsRecentMemberLimit(
        raw: String,
        expected: Int,
    ) {
        // given
        repeat(MEMBERS_FOR_LIMIT) { index ->
            joinedAt(fixture.persistUser(), LocalDateTime.of(2041, 1, 1, 0, 0).plusMinutes(index.toLong()))
        }

        // when
        val result = mockMvc.perform(admin(get("/admin/dashboard/recent-members").param("limit", raw)))

        // then
        result.andExpect(status().isOk).andExpect(jsonPath("$.length()").value(expected))
    }

    @DisplayName("서버 상태 추이 기간은 1~168시간으로 맞추고, 없거나 숫자가 아니면 24시간이다")
    @ParameterizedTest
    @CsvSource("0,1", "169,168", "abc,24", "'',24", "72,72")
    fun clampsServerMetricHours(
        raw: String,
        expected: Int,
    ) {
        // given
        val request = admin(get("/admin/dashboard/server-metrics").param("hours", raw))

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isOk).andExpect(jsonPath("$.hours").value(expected))
    }

    @DisplayName("관리자 대시보드 API는 로그인하지 않으면 401, 일반 회원이면 403이다")
    @ParameterizedTest
    @CsvSource("/admin/dashboard/summary", "/admin/dashboard/recent-members", "/admin/dashboard/server-metrics")
    fun rejectsNonAdmin(path: String) {
        // given
        val anonymous = get(path)
        val member = get(path).with(user("1").roles("USER"))

        // when
        val anonymousResult = mockMvc.perform(anonymous)
        val memberResult = mockMvc.perform(member)

        // then
        anonymousResult.andExpect(status().isUnauthorized)
        memberResult.andExpect(status().isForbidden)
    }

    private fun summary(): JsonNode {
        val result =
            mockMvc
                .perform(admin(get("/admin/dashboard/summary")))
                .andExpect(status().isOk)
                .andReturn()
        return objectMapper.readTree(result.response.contentAsString)
    }

    private fun elements(array: JsonNode): List<JsonNode> = (0 until array.size()).map { array.get(it) }

    private fun admin(request: MockHttpServletRequestBuilder): MockHttpServletRequestBuilder =
        request.with(user("1").roles("ADMIN"))

    private fun joinedAt(
        user: User,
        at: LocalDateTime?,
    ) {
        jdbcTemplate.update("update users set joined_at = ? where id = ?", at, user.id)
    }

    private fun countUsers(): Long = jdbcTemplate.queryForObject("select count(*) from users", Long::class.java) ?: 0L

    private fun visit(
        visitorId: String,
        date: LocalDate,
    ) {
        siteVisitTxService.touchVisitor(visitorId, date)
        siteVisitTxService.touchDailyVisit(visitorId, date, null, null)
    }

    private fun payment(
        amount: BigDecimal,
        approvedAt: LocalDateTime?,
        partialCancel: BigDecimal?,
    ) {
        val slot = fixture.persistOpenSlot(amount)
        val buyer = fixture.persistUser()
        val reservationId = fixture.createReservation(buyer.id, slot).id
        val payment =
            paymentRepository.findByReservationId(reservationId)
                ?: Payment.createPendingForToss(
                    requireNotNull(reservationRepository.findById(reservationId)),
                    "dash-${UUID.randomUUID()}",
                )
        if (approvedAt != null) {
            payment.markSucceeded(approvedAt)
            partialCancel?.let { payment.markPartialRefund(it, approvedAt.plusHours(1)) }
        }
        paymentRepository.save(payment)
    }

    private fun billing(
        amount: Long,
        cycleMonths: Int,
        status: TossBillingSubscriptionStatus,
    ) {
        val owner = fixture.persistUser()
        val method =
            methodRepository.save(
                TossBillingMethod(
                    userId = owner.id,
                    customerKey = "idolglow-user-${owner.id}",
                    billingKey = "bk-${owner.id}",
                    isDefault = true,
                ),
            )
        subscriptionRepository.save(
            TossBillingSubscription(
                userId = owner.id,
                billingMethod = method,
                orderName = BillingDefaults.DEFAULT_ORDER_NAME,
                amount = amount,
                status = status,
                cycleMonths = cycleMonths,
            ),
        )
    }

    companion object {
        private const val MEMBERS_FOR_LIMIT: Int = 21
    }
}
