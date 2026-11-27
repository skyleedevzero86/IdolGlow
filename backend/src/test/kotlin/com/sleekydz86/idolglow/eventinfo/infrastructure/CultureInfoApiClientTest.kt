package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.global.config.CultureInfoApiProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriUtils
import reactor.core.publisher.Mono
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference

@DisplayName("문화정보 API 클라이언트")
class CultureInfoApiClientTest {
    @DisplayName("이미 인코딩된 공공데이터 서비스키를 그대로 유지한다")
    @Test
    fun keepsAlreadyEncodedServiceKey() {

        // given
        val capturedUri = AtomicReference<URI>()
        val client =
            요청Uri를_캡처하는_클라이언트(
                capturedUri = capturedUri,
                serviceKey = "abc%2Bdef%2Fghi%3D%3D",
            )

        // when
        val result =
            client.listArea2Events(
                fromYyyyMMdd = "20260616",
                toYyyyMMdd = "20260616",
                serviceTp = "A",
                sortStdr = 1,
                pageNo = 1,
                numOfrows = 1,
            )

        // then
        assertEquals(1, result.size)
        assertServiceKey가_한_번만_인코딩됨(capturedUri.get(), "abc+def/ghi==")
    }

    @DisplayName("원본 공공데이터 서비스키를 한 번만 인코딩한다")
    @Test
    fun encodesRawServiceKeyOnce() {

        // given
        val capturedUri = AtomicReference<URI>()
        val client =
            요청Uri를_캡처하는_클라이언트(
                capturedUri = capturedUri,
                serviceKey = "abc+def/ghi==",
            )

        // when
        val result =
            client.listArea2Events(
                fromYyyyMMdd = "20260616",
                toYyyyMMdd = "20260616",
                serviceTp = "A",
                sortStdr = 1,
                pageNo = 1,
                numOfrows = 1,
            )

        // then
        assertEquals(1, result.size)
        assertServiceKey가_한_번만_인코딩됨(capturedUri.get(), "abc+def/ghi==")
    }

    @DisplayName("이중 인코딩된 공공데이터 서비스키를 정규화한다")
    @Test
    fun normalizesDoubleEncodedServiceKey() {

        // given
        val capturedUri = AtomicReference<URI>()
        val client =
            요청Uri를_캡처하는_클라이언트(
                capturedUri = capturedUri,
                serviceKey = "abc%252Bdef%252Fghi%253D%253D",
            )

        // when
        val result =
            client.listArea2Events(
                fromYyyyMMdd = "20260616",
                toYyyyMMdd = "20260616",
                serviceTp = "A",
                sortStdr = 1,
                pageNo = 1,
                numOfrows = 1,
            )

        // then
        assertEquals(1, result.size)
        assertServiceKey가_한_번만_인코딩됨(capturedUri.get(), "abc+def/ghi==")
    }

    @DisplayName("하루치 호출이 모두 성공하면 완전한 결과로 표시한다")
    @Test
    fun marksCalendarDayCompleteWhenAllCallsSucceed() {

        // given
        val client = 요청Uri를_캡처하는_클라이언트(AtomicReference(), serviceKey = "abc")

        // when
        val result = client.listEventsForCalendarDayWithCompleteness("20260616", pageNo = 1, numOfRows = 3)

        // then
        assertTrue(result.complete)
        assertTrue(result.events.isNotEmpty())
    }

    @DisplayName("요청 한도 초과로 호출이 빠지면 불완전한 결과로 표시한다")
    @Test
    fun marksCalendarDayIncompleteWhenRateLimited() {

        // given
        val client = 상태코드만_돌려주는_클라이언트(HttpStatus.TOO_MANY_REQUESTS)

        // when
        val result = client.listEventsForCalendarDayWithCompleteness("20260616", pageNo = 1, numOfRows = 3)

        // then
        assertFalse(result.complete)
        assertTrue(result.events.isEmpty())
    }

    @DisplayName("하루치 조회의 연속 호출은 공급처 초당 한도 아래로 150ms 이상 간격을 두고 보낸다")
    @Test
    fun pacesConsecutiveCallsBelowProviderRateLimit() {
        // given
        val startedAt = mutableListOf<Long>()
        val webClient =
            WebClient
                .builder()
                .exchangeFunction {
                    synchronized(startedAt) { startedAt += System.nanoTime() }
                    Mono.just(ClientResponse.create(HttpStatus.OK).body(EMPTY_RESPONSE).build())
                }.build()
        val client = CultureInfoApiClient(webClient, CultureInfoApiProperties(serviceKey = "abc"))

        // when
        client.listEventsForCalendarDayWithCompleteness("20260616", pageNo = 1, numOfRows = 3)

        // then
        val gapsMillis = startedAt.zipWithNext { previous, next -> (next - previous) / NANOS_PER_MILLI }
        assertTrue(gapsMillis.size >= MIN_CALLS, "호출 수 ${gapsMillis.size + 1}")
        assertTrue(gapsMillis.min() >= MIN_GAP_MILLIS, "가장 짧은 간격 ${gapsMillis.min()}ms")
    }

    @DisplayName("서비스키가 비어 있으면 불완전한 결과로 표시한다")
    @Test
    fun marksCalendarDayIncompleteWhenServiceKeyIsBlank() {

        // given
        val client = 요청Uri를_캡처하는_클라이언트(AtomicReference(), serviceKey = "")

        // when
        val result = client.listEventsForCalendarDayWithCompleteness("20260616", pageNo = 1, numOfRows = 3)

        // then
        assertFalse(result.complete)
    }

    @DisplayName("공급처가 HTML 기호로 보낸 제목과 장소는 원래 글자로 풀어 돌려준다")
    @Test
    fun decodesHtmlEntitiesInTitleAndPlace() {
        // given
        val client =
            본문을_돌려주는_클라이언트(
                """
                <response>
                  <header><resultCode>00</resultCode></header>
                  <body>
                    <items>
                      <item>
                        <seq>777</seq>
                        <title>연극 &amp;lt;먼지여자&amp;gt; &amp;middot; 앙코르</title>
                        <place>대학로&amp;nbsp;극장</place>
                        <startDate>20260616</startDate>
                        <endDate>20260616</endDate>
                      </item>
                    </items>
                  </body>
                </response>
                """.trimIndent(),
            )

        // when
        val event = client.listEventsForCalendarDay("20260616", pageNo = 1, numOfRows = 3).first()

        // then
        assertEquals("연극 <먼지여자> · 앙코르", event.title)
        assertEquals("대학로 극장", event.address)
    }

    private fun 본문을_돌려주는_클라이언트(body: String): CultureInfoApiClient {
        val webClient =
            WebClient
                .builder()
                .exchangeFunction { Mono.just(ClientResponse.create(HttpStatus.OK).body(body).build()) }
                .build()
        return CultureInfoApiClient(
            webClient = webClient,
            properties = CultureInfoApiProperties(serviceKey = "abc"),
        )
    }

    private fun 상태코드만_돌려주는_클라이언트(status: HttpStatus): CultureInfoApiClient {
        val webClient =
            WebClient
                .builder()
                .exchangeFunction { Mono.just(ClientResponse.create(status).body("").build()) }
                .build()
        return CultureInfoApiClient(
            webClient = webClient,
            properties = CultureInfoApiProperties(serviceKey = "abc"),
        )
    }

    private fun assertServiceKey가_한_번만_인코딩됨(
        uri: URI,
        expectedPlain: String,
    ) {
        val url = uri.toString()
        assertFalse(url.contains("%252B"), "이중 인코딩이 감지되었습니다: $url")

        val serviceKeyValue =
            Regex("""[?&]serviceKey=([^&]+)""")
                .find(url)
                ?.groupValues
                ?.get(1)
                ?: error("serviceKey 파라미터를 찾을 수 없습니다: $url")

        val decoded = UriUtils.decode(serviceKeyValue, StandardCharsets.UTF_8)
        assertEquals(expectedPlain, decoded, "serviceKey 디코딩 결과가 다릅니다: $url")
    }

    private fun 요청Uri를_캡처하는_클라이언트(
        capturedUri: AtomicReference<URI>,
        serviceKey: String,
    ): CultureInfoApiClient {
        val webClient =
            WebClient
                .builder()
                .exchangeFunction { request ->
                    capturedUri.set(request.url())
                    Mono.just(
                        ClientResponse
                            .create(HttpStatus.OK)
                            .body(
                                """
                                <response>
                                  <header><resultCode>00</resultCode></header>
                                  <body>
                                    <items>
                                      <item>
                                        <seq>123</seq>
                                        <title>Culture Day</title>
                                        <startDate>20260616</startDate>
                                        <endDate>20260616</endDate>
                                        <serviceName>Performance</serviceName>
                                      </item>
                                    </items>
                                  </body>
                                </response>
                                """.trimIndent(),
                            ).build(),
                    )
                }.build()

        return CultureInfoApiClient(
            webClient = webClient,
            properties = CultureInfoApiProperties(serviceKey = serviceKey),
        )
    }

    companion object {
        private const val EMPTY_RESPONSE: String =
            "<response><header><resultCode>00</resultCode></header><body><items></items></body></response>"
        private const val NANOS_PER_MILLI: Long = 1_000_000
        private const val MIN_CALLS: Int = 10
        private const val MIN_GAP_MILLIS: Long = 140
    }
}
