package com.sleekydz86.idolglow.subscription.adapter.web

import com.ninjasquad.springmockk.MockkBean
import com.sleekydz86.idolglow.global.adapter.security.JwtProvider
import com.sleekydz86.idolglow.subscription.application.UnsubscribeTokenService
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailSubscriptionPort
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailConsentSource
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailPreferredBasis
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["app.mail.enabled=true"],
)
@ActiveProfiles("test")
@DisplayName("상품 추천 메일 구독·수신 거부 API")
class ProductRecommendationMailApiTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var subscriptionPort: ProductRecommendationMailSubscriptionPort

    @Autowired
    private lateinit var unsubscribeTokenService: UnsubscribeTokenService

    @MockkBean
    private lateinit var outboundMailPort: OutboundMailPort

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        clearMocks(outboundMailPort)
        every { outboundMailPort.send(any()) } just runs
        webTestClient =
            WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:$port")
                .build()
    }

    @DisplayName("광고성 정보 수신 동의 없이 구독하면 400을 돌려주고 구독을 만들지 않는다")
    @Test
    fun subscribeRequiresAdvertisingConsent() {
        // given
        val user = newUser()
        val refusedBody = """{"agreedToAdvertising":false,"preferredBasis":"AUTO","consentSource":"MYPAGE"}"""
        val missingConsentBody = """{"preferredBasis":"AUTO"}"""

        // when
        val refused = putMine(user, refusedBody)
        val missingConsent = putMine(user, missingConsentBody)

        // then
        refused.expectStatus().isBadRequest
        missingConsent.expectStatus().isBadRequest
        assertNull(subscriptionPort.findByUserId(user.id))
    }

    @DisplayName("동의하면 구독되고 조회·수신 거부가 로그인 회원 기준으로 동작한다")
    @Test
    fun subscribeGetAndDeleteForLoggedInMember() {
        // given
        val user = newUser()
        val body = """{"agreedToAdvertising":true,"preferredBasis":"POPULAR","consentSource":"MY_SURVEY"}"""

        // when
        val before = meRequest(HttpMethod.GET, user)
        val subscribed = putMine(user, body)
        val unsubscribed = meRequest(HttpMethod.DELETE, user)
        val unsubscribedAgain = meRequest(HttpMethod.DELETE, user)

        // then
        before
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.active")
            .isEqualTo(false)
            .jsonPath("$.hasUserSurvey")
            .isEqualTo(false)
            .jsonPath("$.expectedBasis")
            .isEqualTo("POPULAR")
        subscribed
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.active")
            .isEqualTo(true)
            .jsonPath("$.preferredBasis")
            .isEqualTo("POPULAR")
            .jsonPath("$.consentSource")
            .isEqualTo("MY_SURVEY")
            .jsonPath("$.consentedAt")
            .exists()
            .jsonPath("$.maskedEmail")
            .value<String> { assertTrue(it.contains("***@test.com")) }
        unsubscribed
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.active")
            .isEqualTo(false)
            .jsonPath("$.unsubscribedAt")
            .exists()
        unsubscribedAgain.expectStatus().isOk
        assertFalse(requireNotNull(subscriptionPort.findByUserId(user.id)).active)
        verify(exactly = 1) {
            outboundMailPort.send(match { it.to == user.email && !it.subject.contains("(광고)") })
        }
    }

    @DisplayName("로그인 없이 토큰으로 수신 거부하면 구독이 꺼지고 확인 메일이 한 통 간다")
    @Test
    fun unsubscribeByTokenWithoutLogin() {
        // given
        val user = newUser()
        val subscription = subscribe(user)
        val token = unsubscribeTokenService.issue(subscription)

        // when
        val first = postUnsubscribe(token)
        val second = postUnsubscribe(token)

        // then
        assertTrue(first.contains("\"processed\":true"))
        assertEquals(first, second)
        val reloaded = requireNotNull(subscriptionPort.findByUserId(user.id))
        assertFalse(reloaded.active)
        assertEquals(subscription.tokenVersion, reloaded.tokenVersion)
        verify(exactly = 1) {
            outboundMailPort.send(
                match {
                    it.to == user.email &&
                        !it.subject.contains("(광고)") &&
                        it.plainTextBody.contains("수신 거부가 완료")
                },
            )
        }
    }

    @DisplayName("틀린 토큰도 맞는 토큰과 같은 200 응답을 받고 아무 구독도 바뀌지 않는다")
    @Test
    fun wrongTokenReturnsSameResponse() {
        // given
        val user = newUser()
        val subscription = subscribe(user)
        val validToken = unsubscribeTokenService.issue(subscription)
        val tamperedToken = unsubscribeTokenService.issue(subscription.id, subscription.tokenVersion + 1)

        // when
        val wrongResponses = listOf("not-a-token", tamperedToken, "").map(::postUnsubscribe)
        val activeAfterWrong = requireNotNull(subscriptionPort.findByUserId(user.id)).active
        val validResponse = postUnsubscribe(validToken)

        // then
        assertTrue(activeAfterWrong)
        assertFalse(requireNotNull(subscriptionPort.findByUserId(user.id)).active)
        wrongResponses.forEach { assertEquals(validResponse, it) }
        verify(exactly = 1) { outboundMailPort.send(any()) }
    }

    @DisplayName("GET 수신 거부 화면 확인은 상태를 바꾸지 않고 마스킹한 이메일만 보여 준다")
    @Test
    fun getUnsubscribeDoesNotChangeState() {
        // given
        val user = newUser()
        val subscription = subscribe(user)
        val token = unsubscribeTokenService.issue(subscription)

        // when
        val previews = List(2) { getUnsubscribe(token) }
        val wrongPreview = getUnsubscribe("wrong")

        // then
        previews.forEach { preview ->
            preview
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.valid")
                .isEqualTo(true)
                .jsonPath("$.alreadyUnsubscribed")
                .isEqualTo(false)
                .jsonPath("$.maskedEmail")
                .value<String> {
                    assertNotEquals(user.email, it)
                    assertTrue(it.endsWith("@test.com"))
                    assertTrue(it.contains("***"))
                }
        }
        wrongPreview
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.valid")
            .isEqualTo(false)
            .jsonPath("$.maskedEmail")
            .doesNotExist()
        val reloaded = requireNotNull(subscriptionPort.findByUserId(user.id))
        assertTrue(reloaded.active)
        assertNull(reloaded.unsubscribedAt)
        verify(exactly = 0) { outboundMailPort.send(any()) }
    }

    @DisplayName("RFC 8058 원클릭 수신 거부는 List-Unsubscribe=One-Click 폼 본문을 받아 거부 처리한다")
    @Test
    fun oneClickUnsubscribeAcceptsRfc8058Body() {
        // given
        val user = newUser()
        val subscription = subscribe(user)
        val token = unsubscribeTokenService.issue(subscription)

        // when
        val wrongBody = postOneClick(token, "List-Unsubscribe=Something-Else")
        val activeAfterWrongBody = requireNotNull(subscriptionPort.findByUserId(user.id)).active
        val oneClick = postOneClick(token, "List-Unsubscribe=One-Click")

        // then
        wrongBody.expectStatus().isBadRequest
        assertTrue(activeAfterWrongBody)
        oneClick
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.processed")
            .isEqualTo(true)
        assertFalse(requireNotNull(subscriptionPort.findByUserId(user.id)).active)
        verify(exactly = 1) { outboundMailPort.send(match { it.to == user.email }) }
    }

    @DisplayName("수신 거부 세 경로는 로그인 없이 열리고 /me 경로는 로그인이 필요하다")
    @Test
    fun unsubscribePathsArePermitAllButMePathsRequireLogin() {
        // given
        val meMethods = listOf(HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE)

        // when
        val meResponses =
            meMethods.map { method ->
                webTestClient
                    .method(method)
                    .uri("/me/recommendation-mail")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""{"agreedToAdvertising":true}""")
                    .exchange()
            }
        val preview = getUnsubscribe("x")
        val unsubscribe =
            webTestClient
                .post()
                .uri("/recommendation-mail/unsubscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"token":"x"}""")
                .exchange()
        val oneClick = postOneClick("x", "List-Unsubscribe=One-Click")
        val otherPath =
            webTestClient
                .post()
                .uri("/recommendation-mail/unsubscribe/other")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()

        // then
        meResponses.forEach { it.expectStatus().isUnauthorized }
        preview.expectStatus().isOk
        unsubscribe.expectStatus().isOk
        oneClick.expectStatus().isOk
        otherPath.expectStatus().isUnauthorized
    }

    @DisplayName("GraphQL로 추천 메일을 조회·구독·거부할 수 있고 동의 없이는 오류를 낸다")
    @Test
    fun graphQlQueryAndMutations() {
        // given
        val user = newUser()
        val refuse = """mutation { subscribeRecommendationMail(input: { agreedToAdvertising: false }) { active } }"""
        val subscribe =
            """
            mutation {
              subscribeRecommendationMail(
                input: { agreedToAdvertising: true, preferredBasis: AUTO, consentSource: MY_SURVEY }
              ) { active preferredBasis consentSource consentedAt expectedBasis }
            }
            """.trimIndent()

        // when
        val refused = graphQl(user, refuse)
        val subscriptionAfterRefuse = subscriptionPort.findByUserId(user.id)
        val subscribed = graphQl(user, subscribe)
        val mine = graphQl(user, """{ myRecommendationMail { active hasUserSurvey maskedEmail } }""")
        val unsubscribed = graphQl(user, """mutation { unsubscribeRecommendationMail { active unsubscribedAt } }""")

        // then
        refused.jsonPath("$.errors").exists()
        assertNull(subscriptionAfterRefuse)
        subscribed
            .jsonPath("$.errors")
            .doesNotExist()
            .jsonPath("$.data.subscribeRecommendationMail.active")
            .isEqualTo(true)
            .jsonPath("$.data.subscribeRecommendationMail.consentSource")
            .isEqualTo("MY_SURVEY")
            .jsonPath("$.data.subscribeRecommendationMail.expectedBasis")
            .isEqualTo("POPULAR")
        mine
            .jsonPath("$.data.myRecommendationMail.active")
            .isEqualTo(true)
            .jsonPath("$.data.myRecommendationMail.hasUserSurvey")
            .isEqualTo(false)
        unsubscribed
            .jsonPath("$.data.unsubscribeRecommendationMail.active")
            .isEqualTo(false)
            .jsonPath("$.data.unsubscribeRecommendationMail.unsubscribedAt")
            .exists()
        verify(exactly = 1) { outboundMailPort.send(any()) }
    }

    private fun putMine(
        user: User,
        body: String,
    ): WebTestClient.ResponseSpec =
        webTestClient
            .put()
            .uri("/me/recommendation-mail")
            .header(HttpHeaders.AUTHORIZATION, bearer(user))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()

    private fun meRequest(
        method: HttpMethod,
        user: User,
    ): WebTestClient.ResponseSpec =
        webTestClient
            .method(method)
            .uri("/me/recommendation-mail")
            .header(HttpHeaders.AUTHORIZATION, bearer(user))
            .exchange()

    private fun getUnsubscribe(token: String): WebTestClient.ResponseSpec =
        webTestClient
            .get()
            .uri { it.path("/recommendation-mail/unsubscribe").queryParam("token", token).build() }
            .exchange()

    private fun postOneClick(
        token: String,
        body: String,
    ): WebTestClient.ResponseSpec =
        webTestClient
            .post()
            .uri { it.path("/recommendation-mail/unsubscribe/one-click").queryParam("token", token).build() }
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(body)
            .exchange()

    private fun postUnsubscribe(token: String): String =
        requireNotNull(
            webTestClient
                .post()
                .uri("/recommendation-mail/unsubscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(mapOf("token" to token))
                .exchange()
                .expectStatus()
                .isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody,
        )

    private fun graphQl(
        user: User,
        query: String,
    ): WebTestClient.BodyContentSpec =
        webTestClient
            .post()
            .uri("/graphql")
            .header(HttpHeaders.AUTHORIZATION, bearer(user))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("query" to query))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()

    private fun newUser(): User {
        val suffix = UUID.randomUUID().toString().take(8)
        return userRepository.save(User.of("reco$suffix@test.com", "r$suffix"))
    }

    private fun subscribe(user: User): ProductRecommendationMailSubscription =
        subscriptionPort.save(
            ProductRecommendationMailSubscription.create(
                userId = user.id,
                consentSource = ProductRecommendationMailConsentSource.MYPAGE,
                preferredBasis = ProductRecommendationMailPreferredBasis.AUTO,
                now = LocalDateTime.now(),
            ),
        )

    private fun bearer(user: User): String = "Bearer ${jwtProvider.generateToken(user.id, UserRole.USER).accessToken}"
}
