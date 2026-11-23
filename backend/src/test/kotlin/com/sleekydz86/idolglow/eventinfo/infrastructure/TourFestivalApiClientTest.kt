package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.TourLocaleCallFailedException
import com.sleekydz86.idolglow.global.config.TourApiProperties
import com.sleekydz86.idolglow.global.config.TourKorApiProperties
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

@DisplayName("관광공사 축제 API 클라이언트의 언어별 조회")
class TourFestivalApiClientTest {
    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val requestedPaths = mutableListOf<String>()
    private val requestedQueries = mutableListOf<String>()

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    @DisplayName("언어별 조회가 403이면 0건 대신 실패로 알린다")
    @Test
    fun localeSearchThrowsOnForbidden() {
        // given
        응답을_돌려주는_서버(NOT_REGISTERED_RESPONSE, status = 403)
        val client = 클라이언트()

        // when
        val error =
            assertThrows<TourLocaleCallFailedException> {
                client.searchFestivalInLocale("ja", "20261120", "20261220", pageNo = 1, numOfRows = 100)
            }

        // then
        assertEquals("관광 다국어 API 호출이 실패했습니다. locale=ja pageNo=1", error.message)
        assertTrue(requestedPaths.single().endsWith("/JpnService2/searchFestival2"))
    }

    @DisplayName("화면용 국문 조회는 403이어도 기존처럼 빈 목록을 돌려준다")
    @Test
    fun koreanScreenSearchReturnsEmptyOnForbidden() {
        // given
        응답을_돌려주는_서버(NOT_REGISTERED_RESPONSE, status = 403)
        val client = 클라이언트()

        // when
        val result = client.searchFestival("20261120", "20261220", pageNo = 1, numOfRows = 100)

        // then
        assertTrue(result.isEmpty())
    }

    @DisplayName("언어별 조회가 정상이면 해당 언어 서비스의 축제를 돌려준다")
    @Test
    fun localeSearchReturnsEventsOnSuccess() {
        // given
        응답을_돌려주는_서버(축제_응답("1037621", "Pohang International Fireworks Festival"))
        val client = 클라이언트()

        // when
        val result = client.searchFestivalInLocale("en", "20261120", "20261220", pageNo = 1, numOfRows = 100)

        // then
        assertEquals(listOf("1037621"), result.map { it.contentId })
        assertEquals(129.3775942753, result.single().mapX)
        assertTrue(requestedPaths.single().endsWith("/EngService2/searchFestival2"))
    }

    @DisplayName("일문·중문 조회는 관광 지역 허브 키로 부르고, 국문·영문 조회는 국문 서비스 키로 부른다")
    @Test
    fun japaneseAndChineseUseTourApiServiceKey() {
        // given
        응답을_돌려주는_서버(축제_응답("1037621", "축제"))
        val client = 클라이언트(languageKey = "language-key")

        // when
        listOf("ja", "zh", "en").forEach { client.searchFestivalInLocale(it, "20261120", "20261220", 1, 10) }
        client.searchFestival("20261120", "20261220", pageNo = 1, numOfRows = 10)

        // then
        val usedKeys = requestedQueries.map(::serviceKeyOf)
        assertEquals(listOf("language-key", "language-key", "test-key", "test-key"), usedKeys)
    }

    @DisplayName("관광 지역 허브 키가 비어 있으면 일문 조회도 국문 서비스 키로 부른다")
    @Test
    fun japaneseFallsBackToKoreanKeyWhenTourApiKeyIsBlank() {
        // given
        응답을_돌려주는_서버(축제_응답("1037621", "축제"))
        val client = 클라이언트(languageKey = " ")

        // when
        client.searchFestivalInLocale("ja", "20261120", "20261220", pageNo = 1, numOfRows = 10)

        // then
        assertEquals(listOf("test-key"), requestedQueries.map(::serviceKeyOf))
    }

    private fun serviceKeyOf(query: String): String =
        query.split('&').first { it.startsWith("serviceKey=") }.substringAfter('=')

    private fun 응답을_돌려주는_서버(
        body: String,
        status: Int = 200,
    ) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        server.createContext("/") { exchange ->
            requestedPaths += exchange.requestURI.path
            requestedQueries += exchange.requestURI.rawQuery
            exchange.responseHeaders.add("Content-Type", "application/json; charset=UTF-8")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
    }

    private fun 클라이언트(languageKey: String = ""): TourFestivalApiClient =
        TourFestivalApiClient(
            webClient = WebClient.builder().build(),
            objectMapper = ObjectMapper(),
            tourKorApiProperties =
                TourKorApiProperties(
                    baseUrl = "http://127.0.0.1:${server.address.port}/B551011/KorService2",
                    serviceKey = "test-key",
                ),
            tourApiProperties = TourApiProperties(serviceKey = languageKey),
        )

    private fun 축제_응답(
        contentId: String,
        title: String,
    ): String =
        """{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},""" +
            """"body":{"items":{"item":[{"contentid":"$contentId","title":"$title",""" +
            """"eventstartdate":"20261119","eventenddate":"20261121",""" +
            """"mapx":"129.3775942753","mapy":"36.0530051077"}]}}}}"""

    companion object {
        private const val NOT_REGISTERED_RESPONSE: String =
            """{"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"errMsg":"SERVICE_KEY_IS_NOT_REGISTERED_ERROR",""" +
                """"returnReasonCode":"30"}}}"""
    }
}
