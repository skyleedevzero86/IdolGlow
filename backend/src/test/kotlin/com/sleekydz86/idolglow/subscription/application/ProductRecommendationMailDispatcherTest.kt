package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.productpackage.discovery.application.ProductDiscoveryService
import com.sleekydz86.idolglow.productpackage.discovery.application.RecommendationFallback
import com.sleekydz86.idolglow.productpackage.discovery.application.RecommendationReason
import com.sleekydz86.idolglow.productpackage.discovery.application.RecommendationReasonCode
import com.sleekydz86.idolglow.productpackage.discovery.application.RecommendationSignals
import com.sleekydz86.idolglow.productpackage.discovery.application.dto.ProductRankingResponse
import com.sleekydz86.idolglow.subscription.application.event.NewsletterDispatchRequestedEvent
import com.sleekydz86.idolglow.subscription.application.port.out.EmailSubscriptionPort
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import com.sleekydz86.idolglow.subscription.application.port.out.ProductRecommendationMailSubscriptionPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchHistoryPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchRecipientPort
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailConsentSource
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailPreferredBasis
import com.sleekydz86.idolglow.subscription.domain.ProductRecommendationMailSubscription
import com.sleekydz86.idolglow.subscription.domain.SubscriptionAudience
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchFrequency
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipient
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipientCounts
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipientStatus
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSkipReason
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchStatus
import com.sleekydz86.idolglow.subscription.infrastructure.ProductRecommendationMailMemberAdapter
import com.sleekydz86.idolglow.subscription.infrastructure.ProductRecommendationMailProductAdapter
import com.sleekydz86.idolglow.subscription.infrastructure.SmtpOutboundMailAdapter
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserAccountStatus
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import com.sleekydz86.idolglow.user.user.domain.UserSurveyRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.mail.javamail.JavaMailSender
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Properties
import java.util.concurrent.atomic.AtomicLong

@DisplayName("상품 추천 메일 발송")
class ProductRecommendationMailDispatcherTest {
    private val historyPort = RecoMailHistoryPort()
    private val recipientPort = RecoMailRecipientPort()
    private val subscriptionPort = RecoMailSubscriptionPort()
    private val userRepository = mockk<UserRepository>()
    private val userSurveyRepository = mockk<UserSurveyRepository>()
    private val discovery = mockk<ProductDiscoveryService>()
    private val mail = RecoMailOutbound()
    private val dispatchTxService = SubscriptionDispatchTxService(historyPort, recipientPort)
    private val publicUrls =
        AppPublicUrlProperties(
            publicBaseUrl = "https://api.idolglow.test",
            frontendBaseUrl = "https://idolglow.test",
        )
    private val mailProperties =
        AppMailProperties(
            enabled = true,
            recommendation = AppMailProperties.Recommendation(senderContact = "help@idolglow.test"),
        )
    private val composer = SubscriptionEmailComposer(mailProperties, publicUrls)
    private val dispatcher =
        ProductRecommendationMailDispatcher(
            appMailProperties = mailProperties,
            appPublicUrlProperties = publicUrls,
            subscriptionDispatchTxService = dispatchTxService,
            productRecommendationMailDispatchTxService =
                ProductRecommendationMailDispatchTxService(
                    productRecommendationMailSubscriptionPort = subscriptionPort,
                    productRecommendationMailMemberPort =
                        ProductRecommendationMailMemberAdapter(userRepository, userSurveyRepository),
                    productRecommendationMailProductPort = ProductRecommendationMailProductAdapter(discovery),
                    unsubscribeTokenService = UnsubscribeTokenService(subscriptionPort, "dispatcher-test-secret"),
                ),
            subscriptionEmailComposer = composer,
            outboundMailPort = mail,
        ).also { it.clock = fixedClock(MONDAY_MORNING) }
    private val weeklySlug =
        ProductRecommendationMailSlug.of(SubscriptionDispatchFrequency.WEEKLY, MONDAY_MORNING.toLocalDate())

    @DisplayName("같은 주기 키로는 두 번 보내지 않는다")
    @Test
    fun sameSlugIsNotDispatchedTwice() {
        // given
        member(1L, "a@idolglow.test")
        subscriptionPort.add(subscription(id = 11L, userId = 1L))
        givenPopular((1L..8L).map { ranking(it) })

        // when
        dispatcher.dispatch(SubscriptionDispatchFrequency.WEEKLY)
        dispatcher.dispatch(SubscriptionDispatchFrequency.WEEKLY)

        // then
        val history = historyPort.findByContent(SubscriptionContentType.PRODUCT_RECOMMENDATION, weeklySlug)
        assertEquals(listOf("a@idolglow.test"), mail.messages.map { it.to })
        assertEquals(1L, historyPort.count())
        assertEquals(SubscriptionDispatchStatus.SENT, history?.dispatchStatus)
        assertEquals(1L, history?.successCount)
        assertTrue(weeklySlug.matches(Regex("product-reco-\\d{4}-W\\d{2}")))
    }

    @DisplayName("설문이 있는 AUTO 구독은 회원 신호로 고른 상품과 추천 이유를 받는다")
    @Test
    fun personalizedMailUsesSurveySignalsAndReasons() {
        // given
        member(1L, "fan@idolglow.test", hasSurvey = true)
        subscriptionPort.add(subscription(id = 11L, userId = 1L))
        val signals = RecommendationSignals(conceptTags = setOf("LOVELY"), placeKeywords = listOf("성수"))
        val reasons =
            listOf(
                RecommendationReason(RecommendationReasonCode.CONCEPT_MATCH, "선호 컨셉 '러블리/청순' 태그"),
                RecommendationReason(RecommendationReasonCode.PLACE_MATCH, "방문 장소 '성수'"),
            )
        every { discovery.signalsOf(1L) } returns signals
        every { discovery.findRecommendedProducts(signals, 8) } returns
            (21L..28L).map { ranking(it, reasons = reasons) }

        // when
        dispatcher.dispatch(SubscriptionDispatchFrequency.WEEKLY)

        // then
        val message = mail.messages.single()
        assertEquals("(광고) [IdolGlow] 내 취향 추천 상품", message.subject)
        assertTrue(message.plainTextBody.contains("추천 기준: 내 설문 취향 (선호 컨셉 '러블리/청순' 태그, 방문 장소 '성수')"))
        assertTrue(message.plainTextBody.contains("상품21"))
        assertTrue(message.plainTextBody.contains("상품25"))
        assertTrue(!message.plainTextBody.contains("상품26"))
        verify(exactly = 1) { discovery.findRecommendedProducts(signals, 8) }
        verify(exactly = 0) { discovery.findPopularProducts(any()) }
        assertEquals("21,22,23,24,25", subscriptionPort.findById(11L)?.lastSentProductIds)
        assertTrue(subscriptionPort.findById(11L)?.lastSentAt != null)
    }

    @DisplayName("설문이 없는 회원은 인기 상품을 받고, 설문과 맞는 상품이 없으면 인기순으로 대체한다")
    @Test
    fun userWithoutSurveyGetsPopular() {
        // given
        member(1L, "nosurvey@idolglow.test", hasSurvey = false)
        member(2L, "nomatch@idolglow.test", hasSurvey = true)
        subscriptionPort.add(subscription(id = 11L, userId = 1L))
        subscriptionPort.add(subscription(id = 12L, userId = 2L))
        givenPopular((1L..8L).map { ranking(it) })
        every { discovery.signalsOf(2L) } returns RecommendationSignals.EMPTY
        every { discovery.findRecommendedProducts(RecommendationSignals.EMPTY, 8) } returns
            (1L..8L).map { ranking(it).copy(fallback = RecommendationFallback.POPULAR) }

        // when
        dispatcher.dispatch(SubscriptionDispatchFrequency.WEEKLY)

        // then
        val byRecipient = mail.messages.associateBy { it.to }
        val noSurvey = byRecipient.getValue("nosurvey@idolglow.test")
        val noMatch = byRecipient.getValue("nomatch@idolglow.test")
        assertEquals("(광고) [IdolGlow] 이번 주 인기 상품", noSurvey.subject)
        assertTrue(noSurvey.plainTextBody.contains("추천 기준: 인기순"))
        assertTrue(!noSurvey.plainTextBody.contains("설문과 맞는 상품이 없어"))
        assertEquals("(광고) [IdolGlow] 이번 주 인기 상품", noMatch.subject)
        assertTrue(noMatch.plainTextBody.contains("설문과 맞는 상품이 없어 인기 상품을 보내 드려요."))
        verify(exactly = 0) { discovery.signalsOf(1L) }
    }

    @DisplayName("인기 상품은 발송 1회에 한 번만 계산해서 모두에게 쓴다")
    @Test
    fun popularListIsComputedOncePerDispatch() {
        // given
        (1L..3L).forEach { userId ->
            member(userId, "user$userId@idolglow.test")
            subscriptionPort.add(subscription(id = 10L + userId, userId = userId))
        }
        givenPopular((1L..8L).map { ranking(it) })

        // when
        dispatcher.dispatch(SubscriptionDispatchFrequency.WEEKLY)

        // then
        assertEquals(3, mail.messages.size)
        verify(exactly = 1) { discovery.findPopularProducts(8) }
    }

    @DisplayName("인기순만 받기를 고르면 설문이 있어도 인기 상품을 보낸다")
    @Test
    fun preferredBasisPopularIgnoresSurvey() {
        // given
        member(1L, "popular@idolglow.test", hasSurvey = true)
        subscriptionPort.add(
            subscription(id = 11L, userId = 1L, basis = ProductRecommendationMailPreferredBasis.POPULAR),
        )
        givenPopular((1L..8L).map { ranking(it) })

        // when
        dispatcher.dispatch(SubscriptionDispatchFrequency.WEEKLY)

        // then
        assertEquals("(광고) [IdolGlow] 이번 주 인기 상품", mail.messages.single().subject)
        verify(exactly = 0) { discovery.signalsOf(any()) }
        verify(exactly = 0) { discovery.findRecommendedProducts(any<RecommendationSignals>(), any()) }
    }

    @DisplayName("바로 전에 보낸 상품은 빼되, 빼고 3개 미만이 남으면 빼지 않는다")
    @Test
    fun excludesLastSentProductsUnlessFewerThanThree() {
        // given
        member(1L, "three-left@idolglow.test")
        member(2L, "two-left@idolglow.test")
        subscriptionPort.add(subscription(id = 11L, userId = 1L, lastSent = "1,2,3,4,5"))
        subscriptionPort.add(subscription(id = 12L, userId = 2L, lastSent = "1,2,3,4,5,6"))
        givenPopular((1L..8L).map { ranking(it) })

        // when
        dispatcher.dispatch(SubscriptionDispatchFrequency.WEEKLY)

        // then
        val byRecipient = mail.messages.associateBy { it.to }
        val threeLeft = byRecipient.getValue("three-left@idolglow.test").plainTextBody
        val twoLeft = byRecipient.getValue("two-left@idolglow.test").plainTextBody
        assertEquals(listOf(6L, 7L, 8L), productIdsIn(threeLeft))
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), productIdsIn(twoLeft))
        assertEquals("6,7,8", subscriptionPort.findById(11L)?.lastSentProductIds)
        assertEquals("1,2,3,4,5", subscriptionPort.findById(12L)?.lastSentProductIds)
    }

    @DisplayName("보낼 상품이 0개면 메일 없이 SKIPPED와 NO_PRODUCTS를 남긴다")
    @Test
    fun noProductsMarksRecipientSkipped() {
        // given
        member(1L, "empty@idolglow.test")
        subscriptionPort.add(subscription(id = 11L, userId = 1L))
        givenPopular(emptyList())

        // when
        dispatcher.dispatch(SubscriptionDispatchFrequency.WEEKLY)

        // then
        val history = historyPort.findByContent(SubscriptionContentType.PRODUCT_RECOMMENDATION, weeklySlug)
        val recipient = recipientPort.findByDispatchId(history?.id ?: 0L).single()
        assertTrue(mail.messages.isEmpty())
        assertEquals(SubscriptionDispatchRecipientStatus.SKIPPED, recipient.status)
        assertEquals(SubscriptionDispatchSkipReason.NO_PRODUCTS, recipient.skipReason)
        assertEquals(SubscriptionDispatchStatus.SENT, history?.dispatchStatus)
        assertEquals(1L, history?.recipientCount)
        assertEquals(0L, history?.successCount)
        assertEquals(0L, history?.failureCount)
        assertNull(subscriptionPort.findById(11L)?.lastSentAt)
    }

    @DisplayName("List-Unsubscribe 헤더는 추천 메일에만 붙고 소식지 메일에는 없다")
    @Test
    fun listUnsubscribeHeadersOnlyOnRecommendationMail() {
        // given
        member(1L, "a@idolglow.test")
        subscriptionPort.add(subscription(id = 11L, userId = 1L))
        givenPopular((1L..8L).map { ranking(it) })
        val newsletterService =
            SubscriptionDispatchMailService(
                appMailProperties = mailProperties,
                emailSubscriptionPort = newsletterAudience("reader@idolglow.test"),
                outboundMailPort = mail,
                subscriptionEmailComposer = composer,
                subscriptionDispatchTxService = dispatchTxService,
            )
        val sentMimeMessages = mutableListOf<MimeMessage>()
        val javaMailSender = mockk<JavaMailSender>()
        every { javaMailSender.createMimeMessage() } answers { MimeMessage(Session.getInstance(Properties())) }
        every { javaMailSender.send(capture(sentMimeMessages)) } just runs
        val smtp = SmtpOutboundMailAdapter(javaMailSender, mailProperties)

        // when
        dispatcher.dispatch(SubscriptionDispatchFrequency.WEEKLY)
        newsletterService.dispatchNewsletter(newsletterEvent())
        mail.messages.forEach(smtp::send)

        // then
        val recommendation = mail.messages.single { it.to == "a@idolglow.test" }
        val newsletter = mail.messages.single { it.to == "reader@idolglow.test" }
        val listUnsubscribe = recommendation.headers.getValue(OutboundMailMessage.LIST_UNSUBSCRIBE)
        assertTrue(listUnsubscribe.startsWith("<$ONE_CLICK_URL?token="))
        assertTrue(listUnsubscribe.endsWith(">"))
        assertEquals("List-Unsubscribe=One-Click", recommendation.headers[OutboundMailMessage.LIST_UNSUBSCRIBE_POST])
        assertTrue(recommendation.plainTextBody.contains("$UNSUBSCRIBE_PAGE_URL?token="))
        assertTrue(newsletter.headers.isEmpty())
        val recommendationMime = sentMimeMessages[mail.messages.indexOf(recommendation)]
        val newsletterMime = sentMimeMessages[mail.messages.indexOf(newsletter)]
        assertEquals(listUnsubscribe, recommendationMime.getHeader("List-Unsubscribe", null))
        assertEquals("List-Unsubscribe=One-Click", recommendationMime.getHeader("List-Unsubscribe-Post", null))
        assertNull(newsletterMime.getHeader("List-Unsubscribe"))
        assertNull(newsletterMime.getHeader("List-Unsubscribe-Post"))
    }

    @DisplayName("수신 거부·탈퇴·정지·없는 회원·이메일 형식이 틀린 회원에게는 보내지 않는다")
    @Test
    fun inactiveOrDeletedUserIsNotRecipient() {
        // given
        member(1L, "ok@idolglow.test")
        member(2L, "withdrawn@idolglow.test", status = UserAccountStatus.WITHDRAWN)
        member(3L, "suspended@idolglow.test", status = UserAccountStatus.SUSPENDED)
        member(4L, "pending@idolglow.test", status = UserAccountStatus.PENDING)
        member(5L, "not-an-email")
        member(6L, "unsubscribed@idolglow.test")
        every { userRepository.findById(7L) } returns null
        (1L..7L).forEach { subscriptionPort.add(subscription(id = 10L + it, userId = it)) }
        subscriptionPort.findById(16L)?.unsubscribe(MONDAY_MORNING.minusDays(1))
        givenPopular((1L..8L).map { ranking(it) })

        // when
        dispatcher.dispatch(SubscriptionDispatchFrequency.WEEKLY)

        // then
        val history = historyPort.findByContent(SubscriptionContentType.PRODUCT_RECOMMENDATION, weeklySlug)
        assertEquals(listOf("ok@idolglow.test"), mail.messages.map { it.to })
        assertEquals(1L, history?.recipientCount)
    }

    @DisplayName("멈춘 발송을 이어서 보낼 때 이미 보낸 회원은 건너뛴다")
    @Test
    fun resumeAfterCrashDoesNotResendToSentRecipients() {
        // given
        member(1L, "a@idolglow.test")
        member(2L, "b@idolglow.test")
        subscriptionPort.add(subscription(id = 11L, userId = 1L))
        subscriptionPort.add(subscription(id = 12L, userId = 2L))
        givenPopular((1L..8L).map { ranking(it) })
        val past = LocalDateTime.now().minusMinutes(SubscriptionDispatchHistory.RESUME_AFTER_MINUTES + 1)
        val claimed =
            historyPort.saveAndFlush(
                SubscriptionDispatchHistory.record(
                    contentType = SubscriptionContentType.PRODUCT_RECOMMENDATION,
                    contentSlug = weeklySlug,
                    contentTitle = "상품 추천 메일",
                    contentSummary = null,
                    recipientCount = 0,
                    contentCreatedAt = null,
                    dispatchedAt = past,
                    dispatchStatus = SubscriptionDispatchStatus.SENDING,
                ),
            )
        recipientPort.save(
            SubscriptionDispatchRecipient.create(claimed.id, "a@idolglow.test").also {
                it.applyOutcome(sent = true, error = null, sentAt = past)
            },
        )

        // when
        dispatcher.resume(weeklySlug)
        dispatcher.dispatch(SubscriptionDispatchFrequency.WEEKLY)

        // then
        val history = historyPort.findById(claimed.id)
        assertEquals(listOf("b@idolglow.test"), mail.messages.map { it.to })
        assertEquals(SubscriptionDispatchStatus.SENT, history?.dispatchStatus)
        assertEquals(2L, history?.successCount)
        assertEquals(1L, historyPort.count())
    }

    @DisplayName("밤 9시부터 아침 8시 전에는 이어서 보내기도 하지 않는다")
    @Test
    fun doesNotSendOutsideAdvertisingHours() {
        // given
        member(1L, "a@idolglow.test")
        subscriptionPort.add(subscription(id = 11L, userId = 1L))
        givenPopular((1L..8L).map { ranking(it) })
        val night = MONDAY_MORNING.withHour(21)
        dispatcher.clock = fixedClock(night)

        // when
        dispatcher.dispatch(SubscriptionDispatchFrequency.WEEKLY)
        dispatcher.resume(weeklySlug)

        // then
        assertTrue(mail.messages.isEmpty())
        assertEquals(0L, historyPort.count())
    }

    private fun member(
        userId: Long,
        email: String,
        hasSurvey: Boolean = false,
        status: UserAccountStatus = UserAccountStatus.APPROVED,
    ) {
        val user = User.of(email = email, nickname = "회원$userId")
        user.id = userId
        user.accountStatus = status
        every { userRepository.findById(userId) } returns user
        every { userSurveyRepository.existsByUserId(userId) } returns hasSurvey
    }

    private fun subscription(
        id: Long,
        userId: Long,
        basis: ProductRecommendationMailPreferredBasis = ProductRecommendationMailPreferredBasis.AUTO,
        lastSent: String? = null,
    ): ProductRecommendationMailSubscription =
        ProductRecommendationMailSubscription(
            id = id,
            userId = userId,
            active = true,
            preferredBasis = basis,
            consentedAt = MONDAY_MORNING.minusMonths(1),
            consentSource = ProductRecommendationMailConsentSource.MYPAGE,
            lastSentProductIds = lastSent,
        )

    private fun givenPopular(products: List<ProductRankingResponse>) {
        every { discovery.findPopularProducts(8) } returns products
    }

    private fun ranking(
        id: Long,
        reasons: List<RecommendationReason> = emptyList(),
    ): ProductRankingResponse =
        ProductRankingResponse(
            id = id,
            name = "상품$id",
            description = "설명",
            basePrice = BigDecimal("10000"),
            optionsTotalPrice = BigDecimal("2000"),
            minPrice = BigDecimal("12000"),
            totalPrice = BigDecimal("12000"),
            tagNames = emptyList(),
            wishCount = 0,
            averageRating = BigDecimal.ZERO,
            reviewCount = 0,
            matchedTags = emptyList(),
            reasons = reasons,
        )

    private fun productIdsIn(body: String): List<Long> =
        Regex("https://idolglow.test/products/(\\d+)").findAll(body).map { it.groupValues[1].toLong() }.toList()

    private fun newsletterAudience(email: String): EmailSubscriptionPort {
        val port = mockk<EmailSubscriptionPort>()
        every { port.findActiveEmailsByAudience(SubscriptionAudience.NEWSLETTER) } returns listOf(email)
        return port
    }

    private fun newsletterEvent(): NewsletterDispatchRequestedEvent =
        NewsletterDispatchRequestedEvent(
            slug = "weekly",
            title = "주간",
            summary = "요약",
            imageUrl = "https://idolglow.test/cover.png",
            publishedAt = LocalDate.of(2026, 11, 16),
            tags = emptyList(),
            paragraphs = listOf("본문"),
            contentCreatedAt = LocalDateTime.of(2026, 11, 1, 0, 0),
        )

    companion object {
        private val MONDAY_MORNING: LocalDateTime = LocalDateTime.of(2026, 11, 16, 10, 0)
        private const val ONE_CLICK_URL = "https://api.idolglow.test/recommendation-mail/unsubscribe/one-click"
        private const val UNSUBSCRIBE_PAGE_URL = "https://idolglow.test/recommendation-mail/unsubscribe"

        private fun fixedClock(at: LocalDateTime): Clock =
            Clock.fixed(at.atZone(IdolGlowDateTimes.ZONE).toInstant(), IdolGlowDateTimes.ZONE)
    }
}

private typealias RecoMailSubscription = ProductRecommendationMailSubscription

private typealias RecoMailHistory = SubscriptionDispatchHistory

private class RecoMailOutbound : OutboundMailPort {
    val messages: MutableList<OutboundMailMessage> = mutableListOf()

    override fun send(message: OutboundMailMessage) {
        messages.add(message)
    }
}

private class RecoMailSubscriptionPort : ProductRecommendationMailSubscriptionPort {
    private val byId = linkedMapOf<Long, ProductRecommendationMailSubscription>()

    fun add(subscription: ProductRecommendationMailSubscription) {
        byId[subscription.id] = subscription
    }

    override fun findById(id: Long): ProductRecommendationMailSubscription? = byId[id]

    override fun findByUserId(userId: Long): RecoMailSubscription? = byId.values.firstOrNull { it.userId == userId }

    override fun findAllActive(): List<ProductRecommendationMailSubscription> = byId.values.filter { it.active }

    override fun findAllConsentReminderDue(lastNoticeAtOrBefore: LocalDateTime): List<RecoMailSubscription> =
        byId.values.filter { it.active && (it.consentRemindedAt ?: it.consentedAt) <= lastNoticeAtOrBefore }

    override fun save(subscription: ProductRecommendationMailSubscription): ProductRecommendationMailSubscription {
        byId[subscription.id] = subscription
        return subscription
    }
}

private class RecoMailHistoryPort : SubscriptionDispatchHistoryPort {
    private val nextId = AtomicLong(0)
    private val byKey = linkedMapOf<String, SubscriptionDispatchHistory>()
    private val byId = linkedMapOf<Long, SubscriptionDispatchHistory>()

    override fun findAllByLatest(): List<RecoMailHistory> = byId.values.sortedByDescending { it.dispatchedAt }

    override fun save(history: SubscriptionDispatchHistory): SubscriptionDispatchHistory = persist(history)

    override fun saveAndFlush(history: SubscriptionDispatchHistory): SubscriptionDispatchHistory = persist(history)

    override fun count(): Long = byId.size.toLong()

    override fun existsByContentTypeAndContentSlug(
        contentType: SubscriptionContentType,
        contentSlug: String,
    ): Boolean = findByContent(contentType, contentSlug) != null

    override fun findByContent(
        contentType: SubscriptionContentType,
        contentSlug: String,
    ): SubscriptionDispatchHistory? = byKey["${contentType.name}|$contentSlug"]

    override fun findById(id: Long): SubscriptionDispatchHistory? = byId[id]

    override fun claimResume(
        id: Long,
        now: LocalDateTime,
        staleBefore: LocalDateTime,
    ): Boolean {
        val history = byId[id]
        if (history == null || !history.isStaleSending(staleBefore)) {
            return false
        }
        history.beginAttempt(now)
        return true
    }

    override fun findStaleSending(staleBefore: LocalDateTime): List<SubscriptionDispatchHistory> =
        byId.values.filter { it.isStaleSending(staleBefore) }

    private fun persist(history: SubscriptionDispatchHistory): SubscriptionDispatchHistory {
        val key = "${history.contentType.name}|${history.contentSlug}"
        val current = byKey[key]
        if (current != null && current !== history) {
            throw DataIntegrityViolationException("이미 같은 콘텐츠 발송 이력이 있습니다.")
        }
        if (history.id == 0L) {
            val field = SubscriptionDispatchHistory::class.java.getDeclaredField("id")
            field.isAccessible = true
            field.set(history, nextId.incrementAndGet())
        }
        byKey[key] = history
        byId[history.id] = history
        return history
    }
}

private class RecoMailRecipientPort : SubscriptionDispatchRecipientPort {
    private val nextId = AtomicLong(0)
    private val byKey = linkedMapOf<String, SubscriptionDispatchRecipient>()

    override fun save(recipient: SubscriptionDispatchRecipient): SubscriptionDispatchRecipient {
        if (recipient.id == 0L) {
            val field = SubscriptionDispatchRecipient::class.java.getDeclaredField("id")
            field.isAccessible = true
            field.set(recipient, nextId.incrementAndGet())
        }
        byKey["${recipient.dispatchId}|${recipient.email}"] = recipient
        return recipient
    }

    override fun findByDispatchId(dispatchId: Long): List<SubscriptionDispatchRecipient> =
        byKey.values.filter { it.dispatchId == dispatchId }

    override fun findByDispatchIdAndEmail(
        dispatchId: Long,
        email: String,
    ): SubscriptionDispatchRecipient? = byKey["$dispatchId|$email"]

    override fun countByDispatchIds(dispatchIds: Collection<Long>): Map<Long, SubscriptionDispatchRecipientCounts> =
        byKey.values
            .filter { it.dispatchId in dispatchIds }
            .groupBy { it.dispatchId }
            .mapValues { (_, rows) ->
                rows.fold(SubscriptionDispatchRecipientCounts.EMPTY) { counts, row -> counts.plus(row.status, 1) }
            }
}
