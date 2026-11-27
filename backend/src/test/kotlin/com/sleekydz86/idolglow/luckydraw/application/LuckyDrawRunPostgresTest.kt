package com.sleekydz86.idolglow.luckydraw.application

import com.sleekydz86.idolglow.luckydraw.LuckyDrawTestFixture
import com.sleekydz86.idolglow.luckydraw.domain.DrawCandidate
import com.sleekydz86.idolglow.luckydraw.domain.WeightedDrawer
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("응모 자동 추첨")
class LuckyDrawRunPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var fixture: LuckyDrawTestFixture

    @Autowired private lateinit var reservationFixture: PostgresReservationFixture

    @Autowired private lateinit var luckyDrawRunTxService: LuckyDrawRunTxService

    @Autowired private lateinit var luckyDrawScheduler: LuckyDrawScheduler

    @Autowired private lateinit var luckyDrawSeedSource: LuckyDrawSeedSource

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    private val start = LocalDateTime.of(2031, 6, 1, 11, 0)
    private val end = LocalDateTime.of(2031, 6, 8, 11, 0)
    private val drawAt = LocalDateTime.of(2031, 6, 8, 13, 0)

    @BeforeEach
    fun setUp() {
        fixture.clean()
        jdbcTemplate.execute(
            "create table if not exists shedlock (name varchar(64) primary key, lock_until timestamp not null, " +
                "locked_at timestamp not null, locked_by varchar(255) not null)",
        )
        releaseSchedulerLock()
    }

    @AfterEach
    fun tearDown() {
        fixture.restore()
    }

    @DisplayName("D18 추첨 시각 1초 전에는 뽑지 않고, 추첨 시각에는 스케줄러가 뽑는다")
    @Test
    fun drawsOnlyWhenDue() {
        // given
        val draw = fixture.draw(start, end, drawAt, winnerCount = 2)
        repeat(3) { fixture.entry(draw.id, reservationFixture.persistUser().id, start.toLocalDate()) }

        // when
        fixture.setNow(drawAt.minusSeconds(1))
        luckyDrawScheduler.runDueDraws()
        val before = drawnAt(draw.id)
        releaseSchedulerLock()
        fixture.setNow(drawAt)
        luckyDrawScheduler.runDueDraws()

        // then
        assertNull(before)
        assertNotNull(drawnAt(draw.id))
        assertEquals(2, winnerRows(draw.id))
    }

    @DisplayName("D22 저장한 씨앗과 응모권으로 다시 계산하면 같은 당첨자 순서가 나온다")
    @Test
    fun reproducibleFromStoredSeedAndTickets() {
        // given
        val draw = fixture.draw(start, end, drawAt, winnerCount = 3)
        val users = List(6) { reservationFixture.persistUser().id }
        users.forEachIndexed { index, userId ->
            repeat(index % 3 + 1) { day -> fixture.entry(draw.id, userId, start.toLocalDate().plusDays(day.toLong())) }
        }
        fixture.setNow(drawAt.plusMinutes(1))

        // when
        luckyDrawRunTxService.run(draw.id)

        // then
        val seed =
            jdbcTemplate.queryForObject("select draw_seed from lucky_draws where id = ?", Long::class.java, draw.id)!!
        val candidates =
            jdbcTemplate.query(
                "select user_id, tickets from lucky_draw_tickets where lucky_draw_id = ?",
                { rs, _ -> DrawCandidate(rs.getLong(1), rs.getLong(2)) },
                draw.id,
            )
        assertEquals(6, candidates.size)
        assertEquals(WeightedDrawer.draw(seed, candidates, 3), winners(draw.id))
    }

    @DisplayName("D19 이미 추첨한 추첨을 다시 실행하면 아무것도 바꾸지 않는다")
    @Test
    fun runIsIdempotent() {
        // given
        val draw = fixture.draw(start, end, drawAt, winnerCount = 1)
        repeat(4) { fixture.entry(draw.id, reservationFixture.persistUser().id, start.toLocalDate()) }
        fixture.setNow(drawAt)
        luckyDrawRunTxService.run(draw.id)
        val first = winners(draw.id)

        // when
        val second = luckyDrawRunTxService.run(draw.id)

        // then
        assertFalse(second)
        assertEquals(first, winners(draw.id))
        assertEquals(4, ticketRows(draw.id))
    }

    @DisplayName("R23 두 스레드가 같은 추첨을 동시에 실행해도 당첨자 행은 한 벌만 생긴다")
    @Test
    fun concurrentRunsDrawOnce() {
        // given
        val draw = fixture.draw(start, end, drawAt, winnerCount = 2)
        repeat(5) { fixture.entry(draw.id, reservationFixture.persistUser().id, start.toLocalDate()) }
        fixture.setNow(drawAt)
        val pool = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(1)
        val done = CountDownLatch(2)
        val results = java.util.concurrent.ConcurrentLinkedQueue<Boolean>()

        // when
        repeat(2) {
            pool.submit {
                ready.await()
                results.add(runCatching { luckyDrawRunTxService.run(draw.id) }.getOrDefault(false))
                done.countDown()
            }
        }
        ready.countDown()
        done.await(30, TimeUnit.SECONDS)
        pool.shutdownNow()

        // then
        assertEquals(2, winnerRows(draw.id))
        assertEquals(1, results.count { it })
    }

    @DisplayName("D24 응모자가 없으면 당첨자 없이 추첨을 끝낸다")
    @Test
    fun noEntrants() {
        // given
        val draw = fixture.draw(start, end, drawAt)
        fixture.setNow(drawAt)

        // when
        val drawn = luckyDrawRunTxService.run(draw.id)

        // then
        assertTrue(drawn)
        assertNotNull(drawnAt(draw.id))
        assertEquals(0, winnerRows(draw.id))
    }

    @DisplayName("D25 당첨자마다 LUCKY_DRAW_WON 알림을 만들고, 알림을 끈 회원은 만들지 않으며 추첨 결과는 그대로다")
    @Test
    fun notifiesWinnersAfterCommit() {
        // given
        val draw = fixture.draw(start, end, drawAt, winnerCount = 2)
        val notified = reservationFixture.persistUser().id
        val muted = reservationFixture.persistUser().id
        fixture.entry(draw.id, notified, start.toLocalDate())
        fixture.entry(draw.id, muted, start.toLocalDate())
        jdbcTemplate.update(
            "insert into notification_preferences (user_id, type, enabled, created_at, updated_at) " +
                "values (?, 'LUCKY_DRAW_WON', false, now(), now())",
            muted,
        )
        luckyDrawSeedSource.nextSeedOverride = { 3L }
        fixture.setNow(drawAt)

        // when
        luckyDrawRunTxService.run(draw.id)

        // then
        assertEquals(2, winnerRows(draw.id))
        assertEquals(1, notifications(notified))
        assertEquals(0, notifications(muted))
    }

    private fun releaseSchedulerLock() {
        jdbcTemplate.update(
            "update shedlock set lock_until = timestamp '2000-01-01 00:00:00' where name = 'luckyDrawRunner'",
        )
    }

    private fun drawnAt(drawId: Long): LocalDateTime? =
        jdbcTemplate.queryForObject("select drawn_at from lucky_draws where id = ?", LocalDateTime::class.java, drawId)

    private fun winners(drawId: Long): List<Long> =
        jdbcTemplate.queryForList(
            "select user_id from lucky_draw_winners where lucky_draw_id = ? order by winner_order",
            Long::class.java,
            drawId,
        )

    private fun winnerRows(drawId: Long): Int = winners(drawId).size

    private fun ticketRows(drawId: Long): Int =
        jdbcTemplate.queryForObject(
            "select count(*) from lucky_draw_tickets where lucky_draw_id = ?",
            Int::class.java,
            drawId,
        )!!

    private fun notifications(userId: Long): Int =
        jdbcTemplate.queryForObject(
            "select count(*) from notifications where user_id = ? and type = 'LUCKY_DRAW_WON'",
            Int::class.java,
            userId,
        )!!
}
