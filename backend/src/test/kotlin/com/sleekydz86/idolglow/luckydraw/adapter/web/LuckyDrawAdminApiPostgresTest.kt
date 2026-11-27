package com.sleekydz86.idolglow.luckydraw.adapter.web

import com.sleekydz86.idolglow.global.adapter.security.JwtProvider
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.luckydraw.LuckyDrawTestFixture
import com.sleekydz86.idolglow.luckydraw.application.LuckyDrawAdminService
import com.sleekydz86.idolglow.luckydraw.application.LuckyDrawRunTxService
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDrawDraft
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.junit.jupiter.api.AfterEach
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
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

@DisplayName("응모 관리 API와 공개 조회")
class LuckyDrawAdminApiPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var fixture: LuckyDrawTestFixture

    @Autowired private lateinit var reservationFixture: PostgresReservationFixture

    @Autowired private lateinit var jwtProvider: JwtProvider

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var luckyDrawRunTxService: LuckyDrawRunTxService

    @Autowired private lateinit var luckyDrawAdminService: LuckyDrawAdminService

    private lateinit var mockMvc: MockMvc

    private val now = LocalDateTime.of(2031, 7, 1, 12, 0)

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
        fixture.clean()
        fixture.setNow(now)
    }

    @AfterEach
    fun tearDown() {
        fixture.restore()
    }

    @DisplayName("R9 비로그인은 401, 회원은 403, 관리자는 201 로 등록한다")
    @Test
    fun adminOnly() {
        // given
        val body = body(now.plusDays(1), now.plusDays(2))

        // when
        val anonymous =
            mockMvc.perform(post("/admin/lucky-draws").contentType(MediaType.APPLICATION_JSON).content(body))
        val member = send(post("/admin/lucky-draws"), body, UserRole.USER)
        val admin = send(post("/admin/lucky-draws"), body, UserRole.ADMIN)

        // then
        anonymous.andExpect(status().isUnauthorized)
        member.andExpect(status().isForbidden)
        admin.andExpect(status().isCreated).andExpect(jsonPath("$.status").value("SCHEDULED"))
        assertEquals(1, drawRows())
    }

    @DisplayName("D5 기간이 1초라도 겹치면 409 LUCKY_DRAW_PERIOD_OVERLAP, 맞닿으면 등록한다")
    @Test
    fun overlap() {
        // given
        fixture.draw(now.plusDays(1), now.plusDays(3), now.plusDays(3))

        // when
        val overlapBody = body(now.plusDays(3).minusSeconds(1), now.plusDays(5))
        val overlapping = send(post("/admin/lucky-draws"), overlapBody, UserRole.ADMIN)
        val touching = send(post("/admin/lucky-draws"), body(now.plusDays(3), now.plusDays(5)), UserRole.ADMIN)

        // then
        overlapping
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("LUCKY_DRAW_PERIOD_OVERLAP"))
            .andExpect(jsonPath("$.message").value("응모 기간이 겹치는 추첨이 이미 있습니다."))
        touching.andExpect(status().isCreated)
        assertEquals(2, drawRows())
    }

    @DisplayName("D6 응모가 시작된 추첨은 수정·삭제가 409 LUCKY_DRAW_ALREADY_STARTED 이고, 시작 전 추첨은 수정·삭제된다")
    @Test
    fun editOnlyBeforeStart() {
        // given
        val started = fixture.draw(now.minusDays(1), now.plusDays(1))
        val scheduled = fixture.draw(now.plusDays(2), now.plusDays(4))

        // when
        val startedBody = body(now.minusDays(1), now.plusDays(1))
        val updateStarted = send(put("/admin/lucky-draws/${started.id}"), startedBody, UserRole.ADMIN)
        val deleteStarted = send(delete("/admin/lucky-draws/${started.id}"), null, UserRole.ADMIN)
        val updateScheduled =
            send(
                put("/admin/lucky-draws/${scheduled.id}"),
                body(now.plusDays(2), now.plusDays(6), "바뀐 제목"),
                UserRole.ADMIN,
            )
        val deleteScheduled = send(delete("/admin/lucky-draws/${scheduled.id}"), null, UserRole.ADMIN)

        // then
        listOf(updateStarted, deleteStarted).forEach { result ->
            result.andExpect(status().isConflict).andExpect(jsonPath("$.errorCode").value("LUCKY_DRAW_ALREADY_STARTED"))
        }
        updateScheduled.andExpect(status().isOk).andExpect(jsonPath("$.title").value("바뀐 제목"))
        deleteScheduled.andExpect(status().isNoContent)
        assertEquals(1, drawRows())
    }

    @DisplayName("D1 당첨자 수 11 은 400 BAD_REQUEST 와 한국어 메시지이고 행을 만들지 않는다")
    @Test
    fun validationError() {
        // when
        val invalid = body(now.plusDays(1), now.plusDays(2), winnerCount = 11)
        val result = send(post("/admin/lucky-draws"), invalid, UserRole.ADMIN)

        // then
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("당첨자 수는 1 이상 10 이하여야 합니다."))
        assertEquals(0, drawRows())
    }

    @DisplayName("D26 공개 조회는 응모 중 추첨을 먼저 고르고, 없으면 204 를 돌려준다")
    @Test
    fun currentPrefersOpen() {
        // given
        val empty = mockMvc.perform(get("/lucky-draws/current"))
        fixture.draw(now.plusDays(5), now.plusDays(6))
        val open = fixture.draw(now.minusDays(1), now.plusDays(1))

        // when
        val result = mockMvc.perform(get("/lucky-draws/current"))

        // then
        empty.andExpect(status().isNoContent)
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(open.id))
            .andExpect(jsonPath("$.status").value("OPEN"))
    }

    @DisplayName("D26 응모 중이 없으면 마감된 추첨, 그것도 없으면 가장 가까운 예정 추첨을 고른다")
    @Test
    fun currentFallbacks() {
        // given
        val far = fixture.draw(now.plusDays(9), now.plusDays(10))
        val near = fixture.draw(now.plusDays(2), now.plusDays(3))
        val scheduledPick = mockMvc.perform(get("/lucky-draws/current"))
        val closed = fixture.draw(now.minusDays(3), now.minusDays(1), now.plusDays(1))

        // when
        val closedPick = mockMvc.perform(get("/lucky-draws/current"))

        // then
        scheduledPick.andExpect(jsonPath("$.id").value(near.id)).andExpect(jsonPath("$.status").value("SCHEDULED"))
        closedPick.andExpect(jsonPath("$.id").value(closed.id)).andExpect(jsonPath("$.status").value("CLOSED"))
        assertEquals(true, far.id > 0)
    }

    @DisplayName("D27·D28 추첨이 끝나면 당첨자 닉네임 일부를 가려 공개한다")
    @Test
    fun drawnShowsMaskedWinners() {
        // given
        val draw = fixture.draw(now.minusDays(3), now.minusDays(1), now.minusDays(1))
        val user = reservationFixture.persistUser()
        fixture.entry(draw.id, user.id, now.minusDays(2).toLocalDate())
        luckyDrawRunTxService.run(draw.id)

        // when
        val result = mockMvc.perform(get("/lucky-draws/current"))

        // then
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("DRAWN"))
            .andExpect(jsonPath("$.winners[0]").value(user.nickname.value.take(4) + "***"))
    }

    @DisplayName("R7 관리자 8명이 겹치는 기간을 동시에 등록해도 1건만 저장되고 나머지 7건은 LUCKY_DRAW_PERIOD_OVERLAP")
    @Test
    fun concurrentOverlappingCreates() {
        // given
        val pool = Executors.newFixedThreadPool(8)
        val ready = CountDownLatch(1)
        val done = CountDownLatch(8)
        val rejected = AtomicInteger()

        // when
        repeat(8) { index ->
            pool.submit {
                ready.await()
                val startsAt = now.plusDays(1).plusMinutes(index.toLong())
                val draft = LuckyDrawDraft("동시 등록", "경품", "설명", 1, startsAt, now.plusDays(3), now.plusDays(3))
                runCatching { luckyDrawAdminService.create(draft) }
                    .onFailure { error ->
                        val code = (error as? CustomException)?.getExceptionType()?.errorCode
                        if (code == "LUCKY_DRAW_PERIOD_OVERLAP") {
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
        assertEquals(1, drawRows())
        assertEquals(7, rejected.get())
    }

    private fun send(
        builder: org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder,
        body: String?,
        role: UserRole,
    ): ResultActions {
        val user = reservationFixture.persistUser()
        val token = jwtProvider.generateToken(user.id, role).accessToken
        val request = builder.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        val withBody = if (body == null) request else request.contentType(MediaType.APPLICATION_JSON).content(body)
        return mockMvc.perform(withBody)
    }

    private fun body(
        startsAt: LocalDateTime,
        endsAt: LocalDateTime,
        title: String = "가요대전 응모",
        winnerCount: Int = 1,
    ): String =
        """
        {"title":"$title","prizeName":"티켓 2매","prizeDescription":"2층 지정석","winnerCount":$winnerCount,
         "entryStartsAt":"$startsAt","entryEndsAt":"$endsAt","drawAt":"$endsAt"}
        """.trimIndent()

    private fun drawRows(): Int = jdbcTemplate.queryForObject("select count(*) from lucky_draws", Int::class.java)!!
}
