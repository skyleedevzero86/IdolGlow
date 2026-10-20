package com.sleekydz86.idolglow.architecture

import com.sleekydz86.idolglow.global.adapter.security.JwtProvider
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
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
@DisplayName("Graph Ql Mutation Behavior")
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

    @DisplayName("인증 없이 toggleWish 호출시 오류를 반환한다")
    @Test
    fun toggleWishWithoutAuthReturnsError() {

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

    @DisplayName("인증된 toggleWish 호출은 존재하지 않는 상품이면 오류를 반환한다")
    @Test
    fun toggleWishForMissingProductReturnsError() {

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
