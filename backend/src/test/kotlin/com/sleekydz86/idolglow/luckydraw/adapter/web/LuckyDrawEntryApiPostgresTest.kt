package com.sleekydz86.idolglow.luckydraw.adapter.web

import com.sleekydz86.idolglow.global.adapter.security.JwtProvider
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.luckydraw.LuckyDrawTestFixture
import com.sleekydz86.idolglow.luckydraw.application.LuckyDrawEntryService
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("응모 API")
class LuckyDrawEntryApiPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var fixture: LuckyDrawTestFixture

    @Autowired private lateinit var reservationFixture: PostgresReservationFixture

    @Autowired private lateinit var jwtProvider: JwtProvider

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var luckyDrawEntryService: LuckyDrawEntryService

    private lateinit var mockMvc: MockMvc

    private val start = LocalDateTime.of(2031, 5, 1, 11, 0)
    private val end = LocalDateTime.of(2031, 5, 8, 11, 0)

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
        fixture.clean()
    }

    @AfterEach
    fun tearDown() {
        fixture.restore()
    }

    @DisplayName("R10·R11 첫 응모는 201, 같은 날 두 번째는 409 LUCKY_DRAW_ALREADY_ENTERED 이고 행은 1개다")
    @Test
    fun oncePerDay() {
        // given
        val draw = fixture.draw(start, end)
        val user = reservationFixture.persistUser()
        fixture.setNow(start.plusHours(1))

        // when
        val first = enter(user, draw.id)
        val second = enter(user, draw.id)

        // then
        first
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.entryCount").value(1))
            .andExpect(jsonPath("$.tickets").value(1))
        second
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("LUCKY_DRAW_ALREADY_ENTERED"))
            .andExpect(jsonPath("$.message").value("오늘은 이미 응모했습니다. 내일 다시 응모해 주세요."))
        assertEquals(1, entryRows(draw.id))
    }

    @DisplayName("D11 서울 23:59:59 에 응모하고 다음 날 00:00:00 에 다시 응모하면 둘 다 받는다")
    @Test
    fun nextSeoulDayAllowed() {
        // given
        val draw = fixture.draw(start, end)
        val user = reservationFixture.persistUser()

        // when
        fixture.setNow(LocalDateTime.of(2031, 5, 2, 23, 59, 59))
        val first = enter(user, draw.id)
        fixture.setNow(LocalDateTime.of(2031, 5, 3, 0, 0, 0))
        val second = enter(user, draw.id)

        // then
        first.andExpect(status().isCreated)
        second.andExpect(status().isCreated).andExpect(jsonPath("$.entryCount").value(2))
    }

    @DisplayName("R12 시작 정각은 받고, 시작 1초 전과 종료 정각은 409 LUCKY_DRAW_NOT_OPEN")
    @Test
    fun periodBoundary() {
        // given
        val draw = fixture.draw(start, end)

        // when
        fixture.setNow(start.minusSeconds(1))
        val before = enter(reservationFixture.persistUser(), draw.id)
        fixture.setNow(start)
        val atStart = enter(reservationFixture.persistUser(), draw.id)
        fixture.setNow(end)
        val atEnd = enter(reservationFixture.persistUser(), draw.id)

        // then
        before.andExpect(status().isConflict).andExpect(jsonPath("$.errorCode").value("LUCKY_DRAW_NOT_OPEN"))
        atStart.andExpect(status().isCreated)
        atEnd.andExpect(status().isConflict).andExpect(jsonPath("$.errorCode").value("LUCKY_DRAW_NOT_OPEN"))
        assertEquals(1, entryRows(draw.id))
    }

    @DisplayName("R13 비로그인 응모는 401 이고, 없는 추첨은 404 LUCKY_DRAW_NOT_FOUND")
    @Test
    fun unauthenticatedAndMissing() {
        // given
        val draw = fixture.draw(start, end)
        fixture.setNow(start.plusHours(1))

        // when
        val anonymous = mockMvc.perform(post("/lucky-draws/${draw.id}/entries"))
        val missing = enter(reservationFixture.persistUser(), draw.id + 9999)

        // then
        anonymous.andExpect(status().isUnauthorized)
        missing.andExpect(status().isNotFound).andExpect(jsonPath("$.errorCode").value("LUCKY_DRAW_NOT_FOUND"))
        assertEquals(0, entryRows(draw.id))
    }

    @DisplayName("R14 같은 회원이 같은 날 10번 동시에 응모해도 행은 1개, 나머지 9번은 LUCKY_DRAW_ALREADY_ENTERED")
    @Test
    fun concurrentEntries() {
        // given
        val draw = fixture.draw(start, end)
        val user = reservationFixture.persistUser()
        fixture.setNow(start.plusHours(1))
        val pool = Executors.newFixedThreadPool(10)
        val ready = CountDownLatch(1)
        val done = CountDownLatch(10)
        val rejected = AtomicInteger()

        // when
        repeat(10) {
            pool.submit {
                ready.await()
                runCatching { luckyDrawEntryService.enter(draw.id, user.id) }
                    .onFailure { error ->
                        val code = (error as? CustomException)?.getExceptionType()?.errorCode
                        if (code == "LUCKY_DRAW_ALREADY_ENTERED") {
                            rejected.incrementAndGet()
                        }
                    }
                done.countDown()
            }
        }
        ready.countDown()
        done.await(30, TimeUnit.SECONDS)
        pool.shutdownNow()

        // then
        assertEquals(1, entryRows(draw.id))
        assertEquals(9, rejected.get())
    }

    @DisplayName("D12·D17 응모 2회 + 기간 안 이벤트 결제 1건 = 응모권 12, 환불·기간 밖·이벤트 아님·다른 회원 결제는 빼고 센다")
    @Test
    fun ticketsCountOnlyQualifyingPurchases() {
        // given
        val now = LocalDateTime.now().withNano(0)
        val draw = fixture.draw(now.minusDays(1), now.plusDays(1))
        val user = reservationFixture.persistUser()
        val other = reservationFixture.persistUser()
        fixture.entry(draw.id, user.id, now.minusDays(1).toLocalDate())
        fixture.entry(draw.id, user.id, now.toLocalDate())
        fixture.paidReservation(user.id, now.minusHours(1))
        fixture.paidReservation(user.id, now.minusHours(1), status = "REFUNDED")
        fixture.paidReservation(user.id, now.minusDays(2))
        fixture.paidReservation(user.id, now.minusHours(1), event = false)
        fixture.paidReservation(other.id, now.minusHours(1))
        fixture.setNow(now)

        // when
        val result = mockMvc.perform(get("/lucky-draws/${draw.id}/me").header(HttpHeaders.AUTHORIZATION, bearer(user)))

        // then
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.entryCount").value(2))
            .andExpect(jsonPath("$.purchaseCount").value(1))
            .andExpect(jsonPath("$.tickets").value(12))
            .andExpect(jsonPath("$.enteredToday").value(true))
    }

    @DisplayName("D13 응모하지 않고 구매만 한 회원의 응모권은 0 이다")
    @Test
    fun purchaseWithoutEntryHasNoTickets() {
        // given
        val now = LocalDateTime.now().withNano(0)
        val draw = fixture.draw(now.minusDays(1), now.plusDays(1))
        val user = reservationFixture.persistUser()
        fixture.paidReservation(user.id, now.minusHours(1))
        fixture.setNow(now)

        // when
        val result = mockMvc.perform(get("/lucky-draws/${draw.id}/me").header(HttpHeaders.AUTHORIZATION, bearer(user)))

        // then
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.purchaseCount").value(1))
            .andExpect(jsonPath("$.tickets").value(0))
    }

    @DisplayName("D17 결제한 날 이벤트가 진행 중인 결제만 세고, 기간은 시작 정각 포함·종료 정각 제외다")
    @Test
    fun purchasesCountOnlyActiveEventDayAndHalfOpenWindow() {
        // given
        val now = LocalDateTime.now().withNano(0)
        val today = now.toLocalDate()
        val draw = fixture.draw(now.minusDays(1), now.plusDays(1))
        val user = reservationFixture.persistUser()
        fixture.entry(draw.id, user.id, today)
        fixture.paidReservation(user.id, draw.entryStartsAt)
        fixture.paidReservation(user.id, draw.entryEndsAt)
        fixture.paidReservation(user.id, now.minusHours(1), eventDays = today.plusDays(5)..today.plusDays(10))
        fixture.paidReservation(user.id, now.minusHours(1), eventDays = today.minusDays(10)..today.minusDays(5))
        fixture.setNow(now)

        // when
        val result = mockMvc.perform(get("/lucky-draws/${draw.id}/me").header(HttpHeaders.AUTHORIZATION, bearer(user)))

        // then
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.purchaseCount").value(1))
            .andExpect(jsonPath("$.tickets").value(11))
    }

    private fun enter(
        user: User,
        drawId: Long,
    ): ResultActions =
        mockMvc.perform(post("/lucky-draws/$drawId/entries").header(HttpHeaders.AUTHORIZATION, bearer(user)))

    private fun bearer(user: User): String = "Bearer ${jwtProvider.generateToken(user.id, UserRole.USER).accessToken}"

    private fun entryRows(drawId: Long): Int =
        jdbcTemplate.queryForObject(
            "select count(*) from lucky_draw_entries where lucky_draw_id = ?",
            Int::class.java,
            drawId,
        )!!
}
