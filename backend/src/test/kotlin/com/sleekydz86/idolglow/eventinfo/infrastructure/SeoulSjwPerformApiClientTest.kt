package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.global.config.SeoulSjwApiProperties
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

@DisplayName("세종문화회관 공연 API 클라이언트")
class SeoulSjwPerformApiClientTest {
    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    @DisplayName("응답이 기본 버퍼 한도 256KB보다 커도 공연 목록을 모두 읽는다")
    @Test
    fun readsResponseLargerThanDefaultBuffer() {

        // given
        val body = 공연_목록_응답(rowCount = 100, synopsisLength = 4_000)
        응답을_돌려주는_서버(body)
        val client = 클라이언트()

        // when
        val result = client.list(pageNo = 1, numOfRows = 100)

        // then
        assertTrue(body.toByteArray(StandardCharsets.UTF_8).size > DEFAULT_BUFFER_BYTES)
        assertEquals(100, result.size)
        assertEquals("SEOUL_SJW_PERFORM", result.first().source)
    }

    @DisplayName("수집 배치용 조회는 인증키 오류 응답을 예외로 알린다")
    @Test
    fun listForBatchThrowsOnInvalidKeyResponse() {

        // given
        응답을_돌려주는_서버(INVALID_KEY_RESPONSE)
        val client = 클라이언트()

        // when
        val error = assertThrows<IllegalStateException> { client.listForBatch(pageNo = 1, numOfRows = 100) }

        // then
        assertTrue(error.message.orEmpty().contains("INFO-100"))
    }

    @DisplayName("수집 배치용 조회는 서버 오류를 예외로 알린다")
    @Test
    fun listForBatchThrowsOnServerError() {

        // given
        응답을_돌려주는_서버("", status = 500)
        val client = 클라이언트()

        // when
        val result = runCatching { client.listForBatch(pageNo = 1, numOfRows = 100) }

        // then
        assertTrue(result.isFailure)
    }

    @DisplayName("화면용 조회는 인증키 오류 응답이어도 빈 목록을 돌려준다")
    @Test
    fun listReturnsEmptyOnInvalidKeyResponse() {

        // given
        응답을_돌려주는_서버(INVALID_KEY_RESPONSE)
        val client = 클라이언트()

        // when
        val result = client.list(pageNo = 1, numOfRows = 100)

        // then
        assertTrue(result.isEmpty())
    }

    @DisplayName("수집 배치용 조회는 정상 응답이면 공연 목록을 돌려준다")
    @Test
    fun listForBatchReturnsEventsOnSuccess() {

        // given
        응답을_돌려주는_서버(공연_목록_응답(rowCount = 3, synopsisLength = 10))
        val client = 클라이언트()

        // when
        val result = client.listForBatch(pageNo = 1, numOfRows = 100)

        // then
        assertEquals(3, result.size)
    }

    private fun 응답을_돌려주는_서버(
        body: String,
        status: Int = 200,
    ) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        server.createContext("/") { exchange ->
            exchange.responseHeaders.add("Content-Type", "application/json; charset=UTF-8")
            exchange.sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
    }

    private fun 클라이언트(): SeoulSjwPerformApiClient =
        SeoulSjwPerformApiClient(
            webClient = WebClient.builder().build(),
            seoulSjwApiProperties =
                SeoulSjwApiProperties(
                    baseUrl = "http://127.0.0.1:${server.address.port}",
                    apiKey = "test-key",
                    enabled = true,
                    timeoutSeconds = 10,
                ),
            objectMapper = ObjectMapper(),
        )

    private fun 공연_목록_응답(
        rowCount: Int,
        synopsisLength: Int,
    ): String {
        val synopsis = "가".repeat(synopsisLength)
        val rows =
            (1..rowCount).joinToString(",") { index ->
                """{"PERFORM_IDX":"$index","TITLE":"공연 $index",""" +
                    """"START_DATE":"20261120","END_DATE":"20261130","SYNOPSIS":"$synopsis"}"""
            }
        return """{"SJWPerform":{"list_total_count":$rowCount,""" +
            """"RESULT":{"CODE":"INFO-000","MESSAGE":"정상"},"row":[$rows]}}"""
    }

    companion object {
        private const val DEFAULT_BUFFER_BYTES: Int = 256 * 1024
        private const val INVALID_KEY_RESPONSE: String =
            "<RESULT><CODE>INFO-100</CODE><MESSAGE><![CDATA[인증키가 유효하지 않습니다.]]></MESSAGE></RESULT>"
    }
}
