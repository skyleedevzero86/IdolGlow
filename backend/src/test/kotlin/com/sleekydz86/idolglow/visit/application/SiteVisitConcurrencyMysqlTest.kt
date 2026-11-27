package com.sleekydz86.idolglow.visit.application

import com.sleekydz86.idolglow.support.MysqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("방문 기록 동시 저장 (MySQL)")
class SiteVisitConcurrencyMysqlTest : MysqlIntegrationTest() {
    @Autowired private lateinit var siteVisitService: SiteVisitService

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @DisplayName("같은 방문자가 같은 날 동시에 10번 기록해도 방문자·하루 방문은 한 줄씩이고 실패가 없다")
    @Test
    fun recordsOncePerDayUnderConcurrency() {
        // given
        val visitorId = UUID.randomUUID().toString()
        val threads = 10
        val ready = CountDownLatch(threads)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(threads)

        // when
        val failures =
            (1..threads)
                .map {
                    pool.submit(
                        Callable {
                            ready.countDown()
                            start.await()
                            runCatching { siteVisitService.record(visitorId, null, "203.0.113.9") }.exceptionOrNull()
                        },
                    )
                }.also {
                    ready.await()
                    start.countDown()
                }.mapNotNull { it.get(60, TimeUnit.SECONDS) }
        pool.shutdown()

        // then
        assertEquals(emptyList<Throwable>(), failures)
        assertEquals(1, count("select count(*) from site_visitors where visitor_id = ?", visitorId))
        assertEquals(1, count("select count(*) from site_daily_visits where visitor_id = ?", visitorId))
    }

    private fun count(
        sql: String,
        visitorId: String,
    ): Int = jdbcTemplate.queryForObject(sql, Int::class.java, visitorId) ?: 0
}
