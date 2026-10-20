package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.newsletter.domain.NewsletterRepository
import com.sleekydz86.idolglow.subscription.application.dto.RegisterSubscriptionCommand
import com.sleekydz86.idolglow.subscription.application.port.out.EmailSubscriptionPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchHistoryPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchSchedulePort
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.webzine.domain.WebzineIssueRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime

@DisplayName("Subscription Service")
class SubscriptionServiceTest {
    private val emailSubscriptionPort = mockk<EmailSubscriptionPort>()
    private val subscriptionDispatchHistoryPort = mockk<SubscriptionDispatchHistoryPort>()
    private val subscriptionDispatchSchedulePort = mockk<SubscriptionDispatchSchedulePort>()
    private val newsletterRepository = mockk<NewsletterRepository>()
    private val webzineIssueRepository = mockk<WebzineIssueRepository>()
    private val applicationEventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    private lateinit var service: SubscriptionService

    @BeforeEach
    fun setUp() {
        service =
            SubscriptionService(
                emailSubscriptionPort = emailSubscriptionPort,
                subscriptionDispatchHistoryPort = subscriptionDispatchHistoryPort,
                subscriptionDispatchSchedulePort = subscriptionDispatchSchedulePort,
                newsletterRepository = newsletterRepository,
                webzineIssueRepository = webzineIssueRepository,
                applicationEventPublisher = applicationEventPublisher,
            )
    }

    @DisplayName("이메일과 수신 대상이 없으면 구독에 실패한다")
    @Test
    fun failsWhenEmailAndTargetsMissing() {

        // given
        val command =
            RegisterSubscriptionCommand(
                email = " ",
                subscribeNewsletters = false,
                subscribeIssues = false,
                source = "WEB_MODAL",
            )

        // when / then
        assertThrows(IllegalArgumentException::class.java) {
            service.subscribe(command)
        }
    }

    @DisplayName("신규 이메일 구독을 등록한다")
    @Test
    fun registersNewEmailSubscription() {

        // given
        val savedSlot = slot<EmailSubscription>()
        every { emailSubscriptionPort.findByEmail("fan@example.com") } returns null
        every { emailSubscriptionPort.save(capture(savedSlot)) } answers {
            val captured = savedSlot.captured
            EmailSubscription(
                id = 42L,
                email = captured.email,
                subscribedNewsletters = captured.subscribedNewsletters,
                subscribedIssues = captured.subscribedIssues,
                consentedAt = captured.consentedAt,
                subscribedAt = captured.subscribedAt,
                subscriptionSource = captured.subscriptionSource,
                active = captured.active,
            )
        }

        // when
        val result =
            service.subscribe(
                RegisterSubscriptionCommand(
                    email = "  Fan@Example.com ",
                    subscribeNewsletters = true,
                    subscribeIssues = false,
                    source = "WEB_MODAL",
                ),
            )

        // then
        assertEquals(42L, result.id)
        assertEquals("fan@example.com", result.email)
        assertEquals(listOf("소식지"), result.subscribedTargets)
        assertTrue(result.active)
        assertEquals("fan@example.com", savedSlot.captured.email)
        assertTrue(savedSlot.captured.subscribedNewsletters)
        verify(exactly = 1) { emailSubscriptionPort.save(any()) }
    }

    @DisplayName("기존 구독자는 재구독한다")
    @Test
    fun resubscribesExistingSubscriber() {

        // given
        val existing =
            EmailSubscription(
                id = 7L,
                email = "fan@example.com",
                subscribedNewsletters = false,
                subscribedIssues = true,
                consentedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
                subscribedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
                subscriptionSource = "SIGNUP",
                active = false,
            )
        every { emailSubscriptionPort.findByEmail("fan@example.com") } returns existing
        every { emailSubscriptionPort.save(existing) } returns existing

        // when
        val result =
            service.subscribe(
                RegisterSubscriptionCommand(
                    email = "fan@example.com",
                    subscribeNewsletters = true,
                    subscribeIssues = true,
                    source = "WEB_MODAL",
                ),
            )

        // then
        assertEquals(7L, result.id)
        assertTrue(existing.active)
        assertTrue(existing.subscribedNewsletters)
        assertTrue(existing.subscribedIssues)
        assertEquals("WEB_MODAL", existing.subscriptionSource)
    }
}
