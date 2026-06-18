package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.global.config.CultureInfoApiProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriUtils
import reactor.core.publisher.Mono
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference

class CultureInfoApiClientTest {
    @Test
    fun `이미_인코딩된_공공데이터_서비스키를_그대로_유지한다`() {
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

    @Test
    fun `원본_공공데이터_서비스키를_한_번만_인코딩한다`() {
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

    @Test
    fun `이중_인코딩된_공공데이터_서비스키를_정규화한다`() {
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
}
