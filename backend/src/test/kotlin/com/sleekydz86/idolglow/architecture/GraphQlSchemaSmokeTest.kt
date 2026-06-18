package com.sleekydz86.idolglow.architecture

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GraphQlSchemaSmokeTest {
    @LocalServerPort
    private var port: Int = 0

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun 준비() {
        webTestClient =
            WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:$port")
                .build()
    }

    @Test
    fun `health_쿼리가_정상_응답을_반환한다`() {
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

    @Test
    fun `핵심_mutation_필드가_스키마에_등록되어_있다`() {
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
    }
}
