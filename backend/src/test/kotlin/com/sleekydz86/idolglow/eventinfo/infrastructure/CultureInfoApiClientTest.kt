package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.global.infrastructure.config.CultureInfoApiProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URI
import java.util.concurrent.atomic.AtomicReference

class CultureInfoApiClientTest {

    @Test
    fun `preserves already encoded public data service key`() {
        val capturedUri = AtomicReference<URI>()
        val client = clientWithCapturedUri(
            capturedUri = capturedUri,
            serviceKey = "abc%2Bdef%2Fghi%3D%3D",
        )

        val result = client.listArea2Events(
            fromYyyyMMdd = "20260616",
            toYyyyMMdd = "20260616",
            serviceTp = "A",
            sortStdr = 1,
            pageNo = 1,
            numOfrows = 1,
        )

        assertEquals(1, result.size)
        val query = requireNotNull(capturedUri.get().rawQuery)
        assertTrue(query.contains("serviceKey=abc%2Bdef%2Fghi%3D%3D"), query)
        assertFalse(query.contains("%252B"), query)
    }

    @Test
    fun `encodes raw public data service key once`() {
        val capturedUri = AtomicReference<URI>()
        val client = clientWithCapturedUri(
            capturedUri = capturedUri,
            serviceKey = "abc+def/ghi==",
        )

        val result = client.listArea2Events(
            fromYyyyMMdd = "20260616",
            toYyyyMMdd = "20260616",
            serviceTp = "A",
            sortStdr = 1,
            pageNo = 1,
            numOfrows = 1,
        )

        assertEquals(1, result.size)
        val query = requireNotNull(capturedUri.get().rawQuery)
        assertTrue(query.contains("serviceKey=abc%2Bdef%2Fghi%3D%3D"), query)
        assertFalse(query.contains("%252B"), query)
    }

    @Test
    fun `normalizes double encoded public data service key`() {
        val capturedUri = AtomicReference<URI>()
        val client = clientWithCapturedUri(
            capturedUri = capturedUri,
            serviceKey = "abc%252Bdef%252Fghi%253D%253D",
        )

        val result = client.listArea2Events(
            fromYyyyMMdd = "20260616",
            toYyyyMMdd = "20260616",
            serviceTp = "A",
            sortStdr = 1,
            pageNo = 1,
            numOfrows = 1,
        )

        assertEquals(1, result.size)
        val query = requireNotNull(capturedUri.get().rawQuery)
        assertTrue(query.contains("serviceKey=abc%2Bdef%2Fghi%3D%3D"), query)
        assertFalse(query.contains("%252B"), query)
    }

    private fun clientWithCapturedUri(
        capturedUri: AtomicReference<URI>,
        serviceKey: String,
    ): CultureInfoApiClient {
        val webClient = WebClient.builder()
            .exchangeFunction { request ->
                capturedUri.set(request.url())
                Mono.just(
                    ClientResponse.create(HttpStatus.OK)
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
                        )
                        .build(),
                )
            }
            .build()

        return CultureInfoApiClient(
            webClient = webClient,
            properties = CultureInfoApiProperties(serviceKey = serviceKey),
        )
    }
}
