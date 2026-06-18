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
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime

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

    @Test
    fun `이메일과_수신_대상이_없으면_구독에_실패한다`() {
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

    @Test
    fun `신규_이메일_구독을_등록한다`() {
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

    @Test
    fun `기존_구독자는_재구독한다`() {
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
