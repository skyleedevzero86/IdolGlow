package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.global.config.SeoulSjwApiProperties
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
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

    private fun 응답을_돌려주는_서버(body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        server.createContext("/") { exchange ->
            exchange.responseHeaders.add("Content-Type", "application/json; charset=UTF-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
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
    }
}
