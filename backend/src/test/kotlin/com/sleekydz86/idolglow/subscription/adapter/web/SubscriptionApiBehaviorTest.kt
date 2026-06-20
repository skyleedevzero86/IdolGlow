package com.sleekydz86.idolglow.subscription.adapter.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SubscriptionApiBehaviorTest {
    @LocalServerPort
    private var port: Int = 0

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        webTestClient =
            WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:$port")
                .build()
    }

    @Test
    fun `이메일_구독_API가_신규_구독을_등록한다`() {
        // given
        val body =
            """
            {
              "email": "phase1-subscriber@example.com",
              "agreedToPrivacy": true,
              "subscribeNewsletters": true,
              "subscribeIssues": false
            }
            """.trimIndent()

        // when / then
        webTestClient
            .post()
            .uri("/subscriptions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody()
            .jsonPath("$.email")
            .isEqualTo("phase1-subscriber@example.com")
            .jsonPath("$.active")
            .isEqualTo(true)
            .jsonPath("$.subscribedTargets[0]")
            .isEqualTo("소식지")
    }
}
