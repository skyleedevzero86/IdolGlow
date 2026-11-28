package com.sleekydz86.idolglow.payment.adapter.graphql

import com.sleekydz86.idolglow.global.adapter.security.JwtProvider
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("billing-main GraphQL 조회")
class BillingMainGraphQlQueryTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    @Autowired
    private lateinit var fixture: PostgresReservationFixture

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @DisplayName("로그인하지 않으면 네 조회 모두 오류이고 데이터를 주지 않는다")
    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = [
            "{ adminBillingStats { activeCount } }",
            "{ myGlowPassStats { paidCount } }",
            "{ openReservationSlots(productId: \\\"1\\\") { reservationSlotId } }",
            "{ mySurveyAutoPick { enabled } }",
        ],
    )
    fun anonymousRejected(query: String) {
        // when
        val result = post(query, null)

        // then
        result.expectStatus().isOk.expectBody().jsonPath("$.errors").exists().jsonPath("$.data").doesNotExist()
    }

    @DisplayName("S9 일반 회원이 관리자 구독 통계를 조회하면 오류이고 관리자는 주기 4개를 받는다")
    @Test
    fun adminOnly() {
        // given
        val query = "{ adminBillingStats { byCycle { cycleMonths count } monthlyRevenue } }"

        // when
        val member = post(query, token(UserRole.USER))
        val admin = post(query, token(UserRole.ADMIN))

        // then
        member.expectStatus().isOk.expectBody().jsonPath("$.errors").exists()
        admin
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.errors")
            .doesNotExist()
            .jsonPath("$.data.adminBillingStats.byCycle.length()")
            .isEqualTo(4)
    }

    @DisplayName("M1·A1 회원은 본인 이용 통계와 자동 담기 설정을 REST 와 같은 값으로 받는다")
    @Test
    fun memberQueries() {
        // given
        val user = fixture.persistUser()
        val bearer = jwtProvider.generateToken(user.id, UserRole.USER).accessToken

        // when
        val stats = post("{ myGlowPassStats { paidCount totalPaid membershipSince } }", bearer)
        val autoPick = post("{ mySurveyAutoPick { enabled monthlyLimit candidates { id } } }", bearer)

        // then
        stats
            .expectBody()
            .jsonPath("$.data.myGlowPassStats.paidCount")
            .isEqualTo(0)
            .jsonPath("$.data.myGlowPassStats.totalPaid")
            .isEqualTo("0")
            .jsonPath("$.data.myGlowPassStats.membershipSince")
            .doesNotExist()
        autoPick
            .expectBody()
            .jsonPath("$.data.mySurveyAutoPick.enabled")
            .isEqualTo(false)
            .jsonPath("$.data.mySurveyAutoPick.monthlyLimit")
            .isEqualTo(2)
    }

    @DisplayName("R9 회원은 상품의 다가오는 슬롯과 회원 가격을 받는다")
    @Test
    fun openSlots() {
        // given
        val slot = fixture.persistOpenSlot()
        val user = fixture.persistUser()
        val bearer = jwtProvider.generateToken(user.id, UserRole.USER).accessToken

        // when
        val result =
            post(
                "{ openReservationSlots(productId: \\\"${slot.productId}\\\") " +
                    "{ reservationSlotId available totalPrice } }",
                bearer,
            )

        // then
        result
            .expectBody()
            .jsonPath("$.errors")
            .doesNotExist()
            .jsonPath(
                "$.data.openReservationSlots[?(@.reservationSlotId == '${slot.slotId}')].available",
            )
            .isEqualTo(true)
    }

    private fun token(role: UserRole): String = jwtProvider.generateToken(fixture.persistUser().id, role).accessToken

    private fun post(
        query: String,
        bearer: String?,
    ): WebTestClient.ResponseSpec {
        val request =
            webTestClient
                .post()
                .uri("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
        val withAuth = bearer?.let { request.header(HttpHeaders.AUTHORIZATION, "Bearer $it") } ?: request
        return withAuth.bodyValue("""{"query":"$query"}""").exchange()
    }
}
