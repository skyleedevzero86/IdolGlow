package com.sleekydz86.idolglow.visit.adapter.web

import com.sleekydz86.idolglow.global.support.DashboardTime
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityReport
import com.sleekydz86.idolglow.glowcommunity.infrastructure.GlowCommunityReportJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import com.sleekydz86.idolglow.visit.application.SiteVisitRetentionService
import com.sleekydz86.idolglow.visit.application.SiteVisitTxService
import com.sleekydz86.idolglow.visit.domain.VisitorId
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("사이트 방문 기록 API와 회원 대시보드")
class SiteVisitApiPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var dashboardTime: DashboardTime

    @Autowired private lateinit var siteVisitTxService: SiteVisitTxService

    @Autowired private lateinit var retentionService: SiteVisitRetentionService

    @Autowired private lateinit var reportRepository: GlowCommunityReportJpaRepository

    @Autowired private lateinit var fixture: PostgresReservationFixture

    private lateinit var mockMvc: MockMvc

    private val today: LocalDate = LocalDate.of(2032, 7, 20)

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
        useToday(today)
    }

    @AfterEach
    fun restore() {
        dashboardTime.clock = Clock.system(IdolGlowDateTimes.ZONE)
        dashboardTime.storageZone = ZoneId.systemDefault()
    }

    @DisplayName("쿠키 없이 방문하면 204와 HttpOnly·SameSite=Lax·365일 ig_vid 를 주고, 방문자와 하루 방문이 한 줄씩 생긴다")
    @Test
    fun issuesVisitorCookieOnFirstVisit() {
        // given
        val request = post("/visits")

        // when
        val result = mockMvc.perform(request).andExpect(status().isNoContent).andReturn()

        // then
        val header = result.response.getHeader(HttpHeaders.SET_COOKIE).orEmpty()
        val visitorId = cookieValue(result)
        assertTrue(VisitorId.normalizeOrNull(visitorId) == visitorId, "소문자 UUID 여야 한다: $visitorId")
        assertTrue(header.contains("HttpOnly"))
        assertTrue(header.contains("SameSite=Lax"))
        assertTrue(header.contains("Max-Age=31536000"))
        assertTrue(header.contains("Path=/"))
        assertEquals(1, countVisitors(visitorId))
        assertEquals(1, countDailyVisits(visitorId, today))
    }

    @DisplayName("형식이 틀린 쿠키는 새 값으로 바꾸고 틀린 값은 저장하지 않는다")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "0123456-89ab-cdef-0123-456789abcdef",
            "가나다라마바사-89ab-cdef-0123-456789abcdef",
            "😀1234567-89ab-cdef-0123-456789abcdef",
            " 01234567-89ab-cdef-0123-456789abcdef",
            "01234567-89ab-cdef-0123-456789abcde ",
            "",
        ],
    )
    fun replacesMalformedCookie(raw: String) {
        // given
        val request = post("/visits").cookie(Cookie(VisitorId.COOKIE_NAME, raw))

        // when
        val result = mockMvc.perform(request).andExpect(status().isNoContent).andReturn()

        // then
        val issued = cookieValue(result)
        assertNotEquals(raw, issued)
        assertEquals(issued, VisitorId.normalizeOrNull(issued))
        assertEquals(0, countVisitors(raw))
        assertEquals(1, countVisitors(issued))
    }

    @DisplayName("대문자 UUID 쿠키는 소문자로 받아 같은 방문자로 센다")
    @Test
    fun lowercasesUppercaseCookie() {
        // given
        val lower = UUID.randomUUID().toString()
        mockMvc.perform(post("/visits").cookie(Cookie(VisitorId.COOKIE_NAME, lower)))

        // when
        val result =
            mockMvc
                .perform(post("/visits").cookie(Cookie(VisitorId.COOKIE_NAME, lower.uppercase())))
                .andReturn()

        // then
        assertEquals(lower, cookieValue(result))
        assertEquals(1, countVisitors(lower))
        assertEquals(1, countDailyVisits(lower, today))
    }

    @DisplayName("형식은 맞지만 처음 보는 쿠키는 그 값으로 등록한다")
    @Test
    fun registersUnknownButWellFormedCookie() {
        // given
        val known = UUID.randomUUID().toString()

        // when
        val result = mockMvc.perform(post("/visits").cookie(Cookie(VisitorId.COOKIE_NAME, known))).andReturn()

        // then
        assertEquals(known, cookieValue(result))
        assertEquals(1, countVisitors(known))
    }

    @DisplayName("같은 날 다시 와도 하루 방문은 한 줄이고 IP 는 첫 요청 값을 지킨다. 다음 날은 새 줄이고 첫 방문일은 그대로다")
    @Test
    fun keepsOneDailyRowAndFirstIp() {
        // given
        val visitorId = UUID.randomUUID().toString()
        visitFrom(visitorId, "203.0.113.7")

        // when
        visitFrom(visitorId, "198.51.100.9")
        useToday(today.plusDays(1))
        visitFrom(visitorId, "198.51.100.9")

        // then
        assertEquals(1, countDailyVisits(visitorId, today))
        assertEquals("203.0.113.7", dailyIp(visitorId, today))
        assertEquals(1, countDailyVisits(visitorId, today.plusDays(1)))
        assertEquals(today, firstVisitDate(visitorId))
        assertEquals(today.plusDays(1), lastVisitDate(visitorId))
    }

    @DisplayName("비로그인으로 온 날 로그인해서 다시 오면 회원 번호를 채우고, 다른 회원으로 다시 와도 바꾸지 않는다")
    @Test
    fun attachesFirstLoggedInUserOnly() {
        // given
        val visitorId = UUID.randomUUID().toString()
        mockMvc.perform(post("/visits").cookie(Cookie(VisitorId.COOKIE_NAME, visitorId)))

        // when
        mockMvc.perform(post("/visits").cookie(Cookie(VisitorId.COOKIE_NAME, visitorId)).with(user("501").roles("USER")))
        mockMvc.perform(post("/visits").cookie(Cookie(VisitorId.COOKIE_NAME, visitorId)).with(user("502").roles("USER")))

        // then
        assertEquals(501L, dailyUserId(visitorId, today))
    }

    @DisplayName("같은 방문자가 같은 날 동시에 10번 와도 방문자·하루 방문은 한 줄씩이고 모두 204다")
    @Test
    fun recordsOncePerDayUnderConcurrency() {
        // given
        val visitorId = UUID.randomUUID().toString()
        val threads = 10
        val ready = CountDownLatch(threads)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(threads)

        // when
        val statuses =
            (1..threads)
                .map {
                    pool.submit(
                        Callable {
                            ready.countDown()
                            start.await()
                            mockMvc
                                .perform(post("/visits").cookie(Cookie(VisitorId.COOKIE_NAME, visitorId)))
                                .andReturn()
                                .response.status
                        },
                    )
                }.also {
                    ready.await()
                    start.countDown()
                }.map { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()

        // then
        assertEquals(List(threads) { 204 }, statuses)
        assertEquals(1, countVisitors(visitorId))
        assertEquals(1, countDailyVisits(visitorId, today))
    }

    @DisplayName("보관 정리는 오늘−90일 하루 방문과 365일 오지 않은 방문자를 지우고, 오늘−89일과 364일은 남긴다")
    @Test
    fun purgesExpiredVisits() {
        // given
        val oldDay = UUID.randomUUID().toString()
        val keptDay = UUID.randomUUID().toString()
        siteVisitTxService.touchDailyVisit(oldDay, today.minusDays(90), null, null)
        siteVisitTxService.touchDailyVisit(keptDay, today.minusDays(89), null, null)
        val goneVisitor = UUID.randomUUID().toString()
        val keptVisitor = UUID.randomUUID().toString()
        siteVisitTxService.touchVisitor(goneVisitor, today.minusDays(365))
        siteVisitTxService.touchVisitor(keptVisitor, today.minusDays(364))

        // when
        retentionService.purge()

        // then
        assertEquals(0, countDailyVisits(oldDay, today.minusDays(90)))
        assertEquals(1, countDailyVisits(keptDay, today.minusDays(89)))
        assertEquals(0, countVisitors(goneVisitor))
        assertEquals(1, countVisitors(keptVisitor))
    }

    @DisplayName("내 대시보드는 본인 신고 수와 오늘 포함 90일 안 방문한 날 수만 준다")
    @Test
    fun summarizesOwnDashboard() {
        // given
        val me = fixture.persistUser()
        val other = fixture.persistUser()
        reportRepository.save(GlowCommunityReport.create(postId = 900_001L, userId = me.id, reason = "광고"))
        reportRepository.save(GlowCommunityReport.create(postId = 900_002L, userId = me.id, reason = null))
        reportRepository.save(GlowCommunityReport.create(postId = 900_001L, userId = other.id, reason = "중복"))
        siteVisitTxService.touchDailyVisit(UUID.randomUUID().toString(), today, me.id, null)
        siteVisitTxService.touchDailyVisit(UUID.randomUUID().toString(), today.minusDays(89), me.id, null)
        siteVisitTxService.touchDailyVisit(UUID.randomUUID().toString(), today.minusDays(90), me.id, null)
        siteVisitTxService.touchDailyVisit(UUID.randomUUID().toString(), today, other.id, null)

        // when
        val result = mockMvc.perform(get("/me/dashboard").with(user(me.id.toString()).roles("USER")))

        // then
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.reportCount").value(2))
            .andExpect(jsonPath("$.visitDays").value(2))
    }

    @DisplayName("로그인하지 않으면 내 대시보드는 401이다")
    @Test
    fun rejectsAnonymousDashboard() {
        // given
        val request = get("/me/dashboard")

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isUnauthorized)
    }

    private fun useToday(date: LocalDate) {
        dashboardTime.clock = Clock.fixed(date.atTime(12, 0).atZone(IdolGlowDateTimes.ZONE).toInstant(), IdolGlowDateTimes.ZONE)
    }

    private fun visitFrom(
        visitorId: String,
        ip: String,
    ) {
        mockMvc
            .perform(
                post("/visits").cookie(Cookie(VisitorId.COOKIE_NAME, visitorId)).with { request ->
                    request.remoteAddr = ip
                    request
                },
            ).andExpect(status().isNoContent)
    }

    private fun cookieValue(result: MvcResult): String =
        requireNotNull(result.response.getCookie(VisitorId.COOKIE_NAME)?.value) { "ig_vid 쿠키가 없다" }

    private fun countVisitors(visitorId: String): Int =
        jdbcTemplate.queryForObject("select count(*) from site_visitors where visitor_id = ?", Int::class.java, visitorId) ?: 0

    private fun countDailyVisits(
        visitorId: String,
        date: LocalDate,
    ): Int =
        jdbcTemplate.queryForObject(
            "select count(*) from site_daily_visits where visitor_id = ? and visit_date = ?",
            Int::class.java,
            visitorId,
            date,
        ) ?: 0

    private fun dailyIp(
        visitorId: String,
        date: LocalDate,
    ): String? =
        jdbcTemplate.queryForObject(
            "select ip_address from site_daily_visits where visitor_id = ? and visit_date = ?",
            String::class.java,
            visitorId,
            date,
        )

    private fun dailyUserId(
        visitorId: String,
        date: LocalDate,
    ): Long? =
        jdbcTemplate.queryForObject(
            "select user_id from site_daily_visits where visitor_id = ? and visit_date = ?",
            Long::class.java,
            visitorId,
            date,
        )

    private fun firstVisitDate(visitorId: String): LocalDate? =
        jdbcTemplate.queryForObject(
            "select first_visit_date from site_visitors where visitor_id = ?",
            LocalDate::class.java,
            visitorId,
        )

    private fun lastVisitDate(visitorId: String): LocalDate? =
        jdbcTemplate.queryForObject(
            "select last_visit_date from site_visitors where visitor_id = ?",
            LocalDate::class.java,
            visitorId,
        )
}
