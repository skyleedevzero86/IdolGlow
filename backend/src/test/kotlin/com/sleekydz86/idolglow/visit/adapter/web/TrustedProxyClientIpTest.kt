package com.sleekydz86.idolglow.visit.adapter.web

import com.sleekydz86.idolglow.servermetric.application.HttpResponseCounter
import com.sleekydz86.idolglow.visit.domain.VisitorId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.UUID

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "server.forward-headers-strategy=native",
        "server.tomcat.remoteip.internal-proxies=127\\.0\\.0\\.1",
    ],
)
@ActiveProfiles("test")
@DisplayName("믿는 프록시만 따르는 접속 IP")
class TrustedProxyClientIpTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var httpResponseCounter: HttpResponseCounter

    @DisplayName("만료·위조된 토큰을 붙여 방문해도 204와 쿠키를 준다")
    @Test
    fun acceptsVisitWithBrokenToken() {
        // given
        val brokenToken = "Bearer not.a.valid-token"

        // when
        val result =
            client("http://127.0.0.1:$port")
                .post()
                .uri("/visits")
                .header("Authorization", brokenToken)
                .exchange()

        // then
        result.expectStatus().isNoContent.expectCookie().exists(VisitorId.COOKIE_NAME)
    }

    @DisplayName("실제 서버의 HTTP 응답 수와 500 미만 응답을 Micrometer 지표 이름으로 센다")
    @Test
    fun countsRealHttpResponses() {
        // given
        httpResponseCounter.takeSinceLast()

        // when
        repeat(3) {
            client("http://127.0.0.1:$port").post().uri("/visits").exchange().expectStatus().isNoContent
        }
        val delta = httpResponseCounter.takeSinceLast()

        // then
        assertEquals(true, delta.requests >= 3, "응답 수가 지표에서 읽혀야 한다: $delta")
        assertEquals(0L, delta.errors)
    }

    @DisplayName("믿는 프록시를 거친 방문은 X-Forwarded-For 의 IP 를 남긴다")
    @Test
    fun followsTrustedProxyHeader() {
        // given
        val visitorId = UUID.randomUUID().toString()

        // when
        client("http://127.0.0.1:$port")
            .post()
            .uri("/visits")
            .cookie(VisitorId.COOKIE_NAME, visitorId)
            .header("X-Forwarded-For", "203.0.113.50")
            .exchange()
            .expectStatus()
            .isNoContent

        // then
        assertEquals("203.0.113.50", visitIp(visitorId))
    }

    @DisplayName("믿는 프록시가 아닌 곳이 보낸 X-Forwarded-For 는 무시하고 실제 접속 주소를 남긴다")
    @Test
    fun ignoresSpoofedHeaderFromUntrustedClient() {
        // given
        val visitorId = UUID.randomUUID().toString()

        // when
        client("http://[::1]:$port")
            .post()
            .uri("/visits")
            .cookie(VisitorId.COOKIE_NAME, visitorId)
            .header("X-Forwarded-For", "198.51.100.77")
            .exchange()
            .expectStatus()
            .isNoContent

        // then
        assertEquals("0:0:0:0:0:0:0:1", visitIp(visitorId))
    }

    private fun client(baseUrl: String): WebTestClient = WebTestClient.bindToServer().baseUrl(baseUrl).build()

    private fun visitIp(visitorId: String): String? =
        jdbcTemplate.queryForObject(
            "select ip_address from site_daily_visits where visitor_id = ?",
            String::class.java,
            visitorId,
        )
}
