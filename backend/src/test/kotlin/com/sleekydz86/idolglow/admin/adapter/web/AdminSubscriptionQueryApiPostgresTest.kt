package com.sleekydz86.idolglow.admin.adapter.web

import com.sleekydz86.idolglow.subscription.application.port.out.EmailSubscriptionPort
import com.sleekydz86.idolglow.subscription.application.port.out.MailSendLogPort
import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailSubscriptionPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchHistoryPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchRecipientPort
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.MailSendKind
import com.sleekydz86.idolglow.subscription.domain.MailSendLog
import com.sleekydz86.idolglow.subscription.domain.MailSendStatus
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailConsentSource
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailPreferredBasis
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipient
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSkipReason
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchStatus
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@DisplayName("관리자 구독관리 조회 API")
class AdminSubscriptionQueryApiPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var emailSubscriptionPort: EmailSubscriptionPort

    @Autowired
    private lateinit var recommendationSubscriptionPort: ProductRecommendationMailSubscriptionPort

    @Autowired
    private lateinit var historyPort: SubscriptionDispatchHistoryPort

    @Autowired
    private lateinit var recipientPort: SubscriptionDispatchRecipientPort

    @Autowired
    private lateinit var mailSendLogPort: MailSendLogPort

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    private lateinit var mockMvc: MockMvc

    private val asAdmin = user("1").roles("ADMIN")
    private val asMember = user("2").roles("USER")

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @DisplayName("구독관리 조회 API는 관리자만 쓸 수 있다")
    @Test
    fun endpointsRequireAdmin() {
        // given
        val history = saveHistory(token(), SubscriptionContentType.NEWSLETTER, SubscriptionDispatchStatus.SENT, NOW)
        val log = saveLog("guard${token()}@idolglow.test", MailSendKind.OTHER, MailSendStatus.SENT, NOW, null)
        val paths =
            listOf(
                "/admin/subscriptions",
                "/admin/subscriptions/email-subscribers",
                "/admin/subscriptions/recommendation-subscribers",
                "/admin/subscriptions/dispatches",
                "/admin/subscriptions/dispatches/${history.id}",
                "/admin/subscriptions/mail-logs",
                "/admin/subscriptions/mail-logs/${log.id}",
            )

        // when
        val results =
            paths.map { path ->
                Triple(
                    mockMvc.perform(get(path)),
                    mockMvc.perform(get(path).with(asMember)),
                    mockMvc.perform(get(path).with(asAdmin)),
                )
            }

        // then
        results.forEach { (anonymous, member, admin) ->
            anonymous.andExpect(status().isUnauthorized)
            member.andExpect(status().isForbidden)
            admin.andExpect(status().isOk)
        }
    }

    @DisplayName("이메일 구독자는 이메일 일부·대상·상태로 찾고 페이지로 나누며 이메일을 가린다")
    @Test
    fun searchesEmailSubscribers() {
        // given
        val key = token()
        saveEmailSubscription("ab$key-1@idolglow.test", newsletters = true, issues = false, at = NOW.minusDays(3))
        saveEmailSubscription("ab$key-2@idolglow.test", newsletters = true, issues = true, at = NOW.minusDays(2))
        saveEmailSubscription("ab$key-3@idolglow.test", newsletters = false, issues = true, at = NOW.minusDays(1))

        // when
        val firstPage =
            mockMvc.perform(
                get("/admin/subscriptions/email-subscribers")
                    .param("keyword", key.uppercase())
                    .param("audience", "NEWSLETTER")
                    .param("status", "ACTIVE")
                    .param("page", "0")
                    .param("size", "1")
                    .with(asAdmin),
            )
        val idolglowOnly =
            mockMvc.perform(
                get("/admin/subscriptions/email-subscribers")
                    .param("keyword", key)
                    .param("audience", "IDOLGLOW_ISSUE")
                    .with(asAdmin),
            )
        val inactive =
            mockMvc.perform(
                get("/admin/subscriptions/email-subscribers")
                    .param("keyword", key)
                    .param("status", "INACTIVE")
                    .with(asAdmin),
            )

        // then
        firstPage
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.items[0].email").value("ab***@idolglow.test"))
            .andExpect(jsonPath("$.items[0].newsletter").value(true))
            .andExpect(jsonPath("$.items[0].idolglowIssue").value(true))
            .andExpect(jsonPath("$.items[0].statusLabel").value("구독 중"))
        idolglowOnly.andExpect(jsonPath("$.totalElements").value(2))
        inactive.andExpect(jsonPath("$.totalElements").value(0))
    }

    @DisplayName("상품 추천 구독자는 이메일·닉네임과 상태·추천 기준으로 찾는다")
    @Test
    fun searchesRecommendationSubscribers() {
        // given
        val key = token().take(6)
        val popular = saveMember("rc$key", "p$key")
        val auto = saveMember("rd$key", "a$key")
        saveRecommendation(popular, ProductRecommendationMailPreferredBasis.POPULAR, active = true)
        saveRecommendation(auto, ProductRecommendationMailPreferredBasis.AUTO, active = false)

        // when
        val byNickname =
            mockMvc.perform(
                get("/admin/subscriptions/recommendation-subscribers").param("keyword", "p$key").with(asAdmin),
            )
        val byEmailInactive =
            mockMvc.perform(
                get("/admin/subscriptions/recommendation-subscribers")
                    .param("keyword", key)
                    .param("status", "INACTIVE")
                    .with(asAdmin),
            )
        val byBasis =
            mockMvc.perform(
                get("/admin/subscriptions/recommendation-subscribers")
                    .param("keyword", key)
                    .param("basis", "POPULAR")
                    .with(asAdmin),
            )

        // then
        byNickname
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items[0].nickname").value("p$key"))
            .andExpect(jsonPath("$.items[0].email").value("rc***@idolglow.test"))
            .andExpect(jsonPath("$.items[0].preferredBasis").value("POPULAR"))
            .andExpect(jsonPath("$.items[0].active").value(true))
        byEmailInactive
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items[0].nickname").value("a$key"))
            .andExpect(jsonPath("$.items[0].unsubscribedAt").isNotEmpty)
        byBasis
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items[0].userId").value(popular.id))
    }

    @DisplayName("발송 이력은 제목·슬러그·유형·상태·기간으로 찾고 행마다 수신자 집계를 붙인다")
    @Test
    fun searchesDispatchHistories() {
        // given
        val key = token()
        val january = LocalDateTime.of(2020, 1, 10, 9, 0)
        val partial =
            saveHistory("$key-jan", SubscriptionContentType.NEWSLETTER, PARTIAL_FAILED, january)
        saveHistory(
            "$key-feb",
            SubscriptionContentType.NEWSLETTER,
            SubscriptionDispatchStatus.SENT,
            january.plusMonths(1),
        )
        saveHistory("$key-web", SubscriptionContentType.IDOLGLOW_ISSUE, SubscriptionDispatchStatus.SENT, january)
        saveRecipient(partial.id, "sent@idolglow.test", sent = true)
        saveRecipient(partial.id, "fail@idolglow.test", sent = false)

        // when
        val inJanuary =
            mockMvc.perform(
                get("/admin/subscriptions/dispatches")
                    .param("keyword", key)
                    .param("contentType", "NEWSLETTER")
                    .param("from", "2020-01-01")
                    .param("to", "2020-01-31")
                    .with(asAdmin),
            )
        val sentOnly =
            mockMvc.perform(
                get("/admin/subscriptions/dispatches").param("keyword", key).param("status", "SENT").with(asAdmin),
            )
        val invalidRange =
            mockMvc.perform(
                get("/admin/subscriptions/dispatches")
                    .param("from", "2020-02-01")
                    .param("to", "2020-01-01")
                    .with(asAdmin),
            )

        // then
        inJanuary
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items[0].id").value(partial.id))
            .andExpect(jsonPath("$.items[0].sentCount").value(1))
            .andExpect(jsonPath("$.items[0].failedCount").value(1))
            .andExpect(jsonPath("$.items[0].skippedCount").value(0))
        sentOnly.andExpect(jsonPath("$.totalElements").value(2))
        invalidRange.andExpect(status().isBadRequest)
    }

    @DisplayName("발송 상세는 수신자 검색·상태 필터·페이지와 가장 최근 메일 내용을 돌려준다")
    @Test
    fun returnsDispatchDetail() {
        // given
        val key = token()
        val history =
            saveHistory(key, SubscriptionContentType.PRODUCT_RECOMMENDATION, PARTIAL_FAILED, NOW)
        saveRecipient(history.id, "alpha$key@idolglow.test", sent = true)
        saveRecipient(history.id, "beta$key@idolglow.test", sent = false)
        recipientPort.save(
            SubscriptionDispatchRecipient.create(history.id, "gamma$key@idolglow.test").apply {
                skip(SubscriptionDispatchSkipReason.NO_PRODUCTS)
            },
        )
        saveLog("alpha$key@idolglow.test", MailSendKind.PRODUCT_RECOMMENDATION, MailSendStatus.SENT, NOW, history.id)
        val latest =
            saveLog("beta$key@idolglow.test", RECOMMENDATION, MailSendStatus.FAILED, NOW, history.id)

        // when
        val all =
            mockMvc.perform(
                get("/admin/subscriptions/dispatches/${history.id}").param("size", "2").with(asAdmin),
            )
        val skipped =
            mockMvc.perform(
                get("/admin/subscriptions/dispatches/${history.id}").param("status", "SKIPPED").with(asAdmin),
            )
        val byEmail =
            mockMvc.perform(get("/admin/subscriptions/dispatches/${history.id}").param("keyword", "beta").with(asAdmin))
        val missing = mockMvc.perform(get("/admin/subscriptions/dispatches/987654321").with(asAdmin))

        // then
        all
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.dispatch.id").value(history.id))
            .andExpect(jsonPath("$.dispatch.sentCount").value(1))
            .andExpect(jsonPath("$.dispatch.failedCount").value(1))
            .andExpect(jsonPath("$.dispatch.skippedCount").value(1))
            .andExpect(jsonPath("$.recipients.totalElements").value(3))
            .andExpect(jsonPath("$.recipients.hasNext").value(true))
            .andExpect(jsonPath("$.recipients.items[0].email").value("al***@idolglow.test"))
            .andExpect(jsonPath("$.mailContent.mailLogId").value(latest.id))
            .andExpect(jsonPath("$.mailContent.htmlBody", containsString("token=***")))
            .andExpect(jsonPath("$.mailContent.htmlBody", not(containsString("raw-secret"))))
        skipped
            .andExpect(jsonPath("$.recipients.totalElements").value(1))
            .andExpect(jsonPath("$.recipients.items[0].skipReason").value("NO_PRODUCTS"))
            .andExpect(jsonPath("$.recipients.items[0].skipReasonLabel").value("보낼 상품 없음"))
        byEmail
            .andExpect(jsonPath("$.recipients.totalElements").value(1))
            .andExpect(jsonPath("$.recipients.items[0].status").value("FAILED"))
        missing.andExpect(status().isNotFound)
    }

    @DisplayName("메일 발송 기록은 종류·상태·기간·수신 이메일로 찾고 상세에서 가린 본문을 보여 준다")
    @Test
    fun searchesMailLogs() {
        // given
        val key = token()
        val today = LocalDate.now()
        saveLog("old$key@idolglow.test", MailSendKind.CONSENT_REMINDER, MailSendStatus.SENT, NOW.minusDays(10), null)
        val recent =
            saveLog("new$key@idolglow.test", MailSendKind.CONSENT_REMINDER, MailSendStatus.SENT, NOW.minusDays(1), null)
        saveLog("fail$key@idolglow.test", MailSendKind.AUTH, MailSendStatus.FAILED, NOW.minusDays(1), null)

        // when
        val byKind =
            mockMvc.perform(
                get("/admin/subscriptions/mail-logs")
                    .param("keyword", key)
                    .param("kind", "CONSENT_REMINDER")
                    .param("from", today.minusDays(3).toString())
                    .param("to", today.toString())
                    .with(asAdmin),
            )
        val failed =
            mockMvc.perform(
                get("/admin/subscriptions/mail-logs").param("keyword", key).param("status", "FAILED").with(asAdmin),
            )
        val paged =
            mockMvc.perform(
                get("/admin/subscriptions/mail-logs")
                    .param("keyword", key)
                    .param("page", "1")
                    .param("size", "2")
                    .with(asAdmin),
            )
        val detail = mockMvc.perform(get("/admin/subscriptions/mail-logs/${recent.id}").with(asAdmin))
        val missing = mockMvc.perform(get("/admin/subscriptions/mail-logs/987654321").with(asAdmin))

        // then
        byKind
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items[0].id").value(recent.id))
            .andExpect(jsonPath("$.items[0].recipientEmail").value("ne***@idolglow.test"))
            .andExpect(jsonPath("$.items[0].kindLabel").value("수신 동의 안내"))
            .andExpect(jsonPath("$.items[0].htmlBody").doesNotExist())
        failed
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items[0].status").value("FAILED"))
            .andExpect(jsonPath("$.items[0].sentAt").isEmpty)
        paged
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
        detail
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.subject").value("제목"))
            .andExpect(jsonPath("$.htmlBody", containsString("unsubscribe?token=***")))
            .andExpect(jsonPath("$.textBody", not(containsString("raw-secret"))))
        missing.andExpect(status().isNotFound)
    }

    private fun token(): String =
        UUID
            .randomUUID()
            .toString()
            .replace("-", "")
            .take(10)

    private fun saveEmailSubscription(
        email: String,
        newsletters: Boolean,
        issues: Boolean,
        at: LocalDateTime,
    ): EmailSubscription =
        emailSubscriptionPort.save(
            EmailSubscription.create(
                email = email,
                subscribedNewsletters = newsletters,
                subscribedIssues = issues,
                consentedAt = at,
                subscribedAt = at,
                subscriptionSource = "TEST",
            ),
        )

    private fun saveMember(
        emailLocal: String,
        nickname: String,
    ): User = userJpaRepository.save(User(email = "$emailLocal@idolglow.test", nickname = Nickname.of(nickname)))

    private fun saveRecommendation(
        member: User,
        basis: ProductRecommendationMailPreferredBasis,
        active: Boolean,
    ): ProductRecommendationMailSubscription {
        val subscription =
            ProductRecommendationMailSubscription.create(
                userId = member.id,
                consentSource = ProductRecommendationMailConsentSource.MYPAGE,
                preferredBasis = basis,
                now = NOW,
            )
        if (!active) subscription.unsubscribe(NOW.plusHours(1))
        return recommendationSubscriptionPort.save(subscription)
    }

    private fun saveHistory(
        slug: String,
        contentType: SubscriptionContentType,
        status: SubscriptionDispatchStatus,
        dispatchedAt: LocalDateTime,
    ): SubscriptionDispatchHistory =
        historyPort.saveAndFlush(
            SubscriptionDispatchHistory.record(
                contentType = contentType,
                contentSlug = slug,
                contentTitle = "발송 $slug",
                contentSummary = null,
                recipientCount = 0,
                contentCreatedAt = null,
                dispatchedAt = dispatchedAt,
                dispatchStatus = status,
            ),
        )

    private fun saveRecipient(
        dispatchId: Long,
        email: String,
        sent: Boolean,
    ) {
        val recipient = SubscriptionDispatchRecipient.create(dispatchId, email)
        recipient.applyOutcome(sent, if (sent) null else "SMTP 오류", NOW)
        recipientPort.save(recipient)
    }

    private fun saveLog(
        email: String,
        kind: MailSendKind,
        status: MailSendStatus,
        at: LocalDateTime,
        dispatchId: Long?,
    ): MailSendLog =
        mailSendLogPort.save(
            MailSendLog.record(
                kind = kind,
                recipientEmail = email,
                subject = "제목",
                htmlBody = "<a href=\"$UNSUBSCRIBE_URL\">수신 거부</a>",
                textBody = UNSUBSCRIBE_URL,
                status = status,
                error = if (status == MailSendStatus.FAILED) "SMTP 오류" else null,
                dispatchId = dispatchId,
                now = at,
            ),
        )

    companion object {
        private val NOW: LocalDateTime = LocalDateTime.now().withNano(0)
        private val PARTIAL_FAILED = SubscriptionDispatchStatus.PARTIAL_FAILED
        private val RECOMMENDATION = MailSendKind.PRODUCT_RECOMMENDATION
        private const val UNSUBSCRIBE_URL = "https://idolglow.test/recommendation-mail/unsubscribe?token=raw-secret"
    }
}
