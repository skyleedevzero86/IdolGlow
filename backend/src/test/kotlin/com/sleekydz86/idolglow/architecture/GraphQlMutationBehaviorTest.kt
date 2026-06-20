package com.sleekydz86.idolglow.architecture

import com.sleekydz86.idolglow.global.adapter.security.JwtProvider
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GraphQlMutationBehaviorTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var jwtProvider: JwtProvider

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
    fun `인증_없이_toggleWish_호출시_오류를_반환한다`() {
        // given
        val query =
            """
            {
              "query": "mutation { toggleWish(productId: \"1\") { wished } }"
            }
            """.trimIndent()

        // when / then
        webTestClient
            .post()
            .uri("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(query)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.errors")
            .exists()
    }

    @Test
    fun `인증된_toggleWish_호출은_존재하지_않는_상품이면_오류를_반환한다`() {
        // given
        val token = jwtProvider.generateToken(userId = 999_999L, role = UserRole.USER)
        val query =
            """
            {
              "query": "mutation { toggleWish(productId: \"999999\") { wished } }"
            }
            """.trimIndent()

        // when / then
        webTestClient
            .post()
            .uri("/graphql")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${token.accessToken}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(query)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.errors")
            .exists()
    }
}
