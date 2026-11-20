package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.global.config.KopisApiProperties
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

@DisplayName("KOPIS 공연 API 클라이언트")
class KopisPerformanceApiClientTest {
    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    @DisplayName("수집 배치용 조회는 400 응답을 예외로 알리고 메시지에 인증키를 넣지 않는다")
    @Test
    fun batchThrowsOnBadRequestWithoutKey() {
        // given
        응답을_돌려주는_서버("<error>bad</error>", status = 400)
        val client = 클라이언트(KEY)

        // when
        val error =
            assertThrows<IllegalStateException> {
                client.listFestivalsForBatch("20261120", "20261220", page = 1, rows = 100)
            }

        // then
        assertEquals("KOPIS 호출 실패. path=prffest status=400", error.message)
        assertFalse(error.message.orEmpty().contains(KEY))
    }

    @DisplayName("수집 배치용 조회는 인증키가 비어 있으면 예외로 알린다")
    @Test
    fun batchThrowsWhenKeyIsEmpty() {
        // given
        응답을_돌려주는_서버(공연_목록_응답(2))
        val client = 클라이언트("")

        // when
        val error =
            assertThrows<IllegalStateException> {
                client.listPerformancesForBatch("20261120", "20261220", page = 1, rows = 100)
            }

        // then
        assertEquals("KOPIS API 인증키가 비어 있습니다.", error.message)
    }

    @DisplayName("수집 배치용 조회는 정상 응답이면 공연 목록을 돌려준다")
    @Test
    fun batchReturnsEventsOnSuccess() {
        // given
        응답을_돌려주는_서버(공연_목록_응답(2))
        val client = 클라이언트(KEY)

        // when
        val result = client.listPerformancesForBatch("20261120", "20261220", page = 1, rows = 100)

        // then
        assertEquals(listOf("PF1", "PF2"), result.map { it.contentId })
        assertEquals("KOPIS_PBLPRFR", result.first().source)
        assertEquals("20261120", result.first().eventStartDate)
    }

    @DisplayName("화면용 조회는 400 응답이어도 빈 목록을 돌려준다")
    @Test
    fun screenListReturnsEmptyOnBadRequest() {
        // given
        응답을_돌려주는_서버("<error>bad</error>", status = 400)
        val client = 클라이언트(KEY)

        // when
        val result = client.listPerformances("20261120", "20261220", page = 1, rows = 100)

        // then
        assertTrue(result.isEmpty())
    }

    private fun 응답을_돌려주는_서버(
        body: String,
        status: Int = 200,
    ) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        server.createContext("/") { exchange ->
            exchange.responseHeaders.add("Content-Type", "application/xml; charset=UTF-8")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
    }

    private fun 클라이언트(key: String): KopisPerformanceApiClient =
        KopisPerformanceApiClient(
            webClient = WebClient.builder().build(),
            kopisApiProperties =
                KopisApiProperties(
                    baseUrl = "http://127.0.0.1:${server.address.port}",
                    serviceKey = key,
                ),
        )

    private fun 공연_목록_응답(rowCount: Int): String {
        val rows =
            (1..rowCount).joinToString("") { index ->
                "<db><mt20id>PF$index</mt20id><prfnm>공연 $index</prfnm>" +
                    "<prfpdfrom>2026.11.20</prfpdfrom><prfpdto>2026.12.20</prfpdto></db>"
            }
        return "<dbs>$rows</dbs>"
    }

    companion object {
        private const val KEY: String = "secret-kopis-key"
    }
}
