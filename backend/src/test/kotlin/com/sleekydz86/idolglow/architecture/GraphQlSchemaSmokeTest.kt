package com.sleekydz86.idolglow.architecture

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("GraphQL 스키마")
class GraphQlSchemaSmokeTest {
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

    @DisplayName("health 쿼리가 정상 응답을 반환한다")
    @Test
    fun healthQueryReturnsOk() {

        // given
        val query = """{"query":"{ health }"}"""

        // when
        val exchange =
            webTestClient
                .post()
                .uri("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(query)
                .exchange()

        // then
        exchange
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.health")
            .isEqualTo("정상")
    }

    @DisplayName("핵심 mutation 필드가 스키마에 등록되어 있다")
    @Test
    fun coreMutationFieldsAreRegistered() {

        // given
        val query =
            """
            {
              "query": "{ __schema { mutationType { fields { name } } } }"
            }
            """.trimIndent()

        // when
        val exchange =
            webTestClient
                .post()
                .uri("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(query)
                .exchange()

        // then
        exchange
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.data.__schema.mutationType.fields[?(@.name == 'createReservation')].name")
            .isEqualTo("createReservation")
            .jsonPath("$.data.__schema.mutationType.fields[?(@.name == 'toggleWish')].name")
            .isEqualTo("toggleWish")
            .jsonPath("$.data.__schema.mutationType.fields[?(@.name == 'createProductReview')].name")
            .isEqualTo("createProductReview")
            .jsonPath("$.data.__schema.mutationType.fields[?(@.name == 'cancelReservation')].name")
            .isEqualTo("cancelReservation")
            .jsonPath("$.data.__schema.mutationType.fields[?(@.name == 'registerSlotWaitlist')].name")
            .isEqualTo("registerSlotWaitlist")
    }
}
