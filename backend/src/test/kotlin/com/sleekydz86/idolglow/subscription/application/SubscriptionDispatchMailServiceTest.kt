package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.config.AppMailProperties
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.subscription.application.event.NewsletterDispatchRequestedEvent
import com.sleekydz86.idolglow.subscription.application.port.out.EmailSubscriptionPort
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailMessage
import com.sleekydz86.idolglow.subscription.application.port.out.OutboundMailPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchHistoryPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchRecipientPort
import com.sleekydz86.idolglow.subscription.domain.EmailSubscription
import com.sleekydz86.idolglow.subscription.domain.SubscriptionAudience
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipient
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipientStatus
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class SubscriptionDispatchMailServiceTest {
    @Test
    fun concurrentDispatchSendsOnce() {
        val emails = listOf("a@idolglow.app", "b@idolglow.app")
        val historyPort = InMemorySubscriptionDispatchHistoryPort()
        val mail = RecordingOutboundMail()
        val service = mailService(emails, historyPort, InMemorySubscriptionDispatchRecipientPort(), mail)
        historyPort.findGate = CountDownLatch(2)
        historyPort.saveGate = CountDownLatch(2)
        val start = CountDownLatch(1)
        val done = CountDownLatch(2)
        val pool = Executors.newFixedThreadPool(2)
        repeat(2) {
            pool.submit {
                start.await(5, TimeUnit.SECONDS)
                try {
                    service.dispatchNewsletter(newsletterEvent())
                } finally {
                    done.countDown()
                }
            }
        }
        start.countDown()
        assertTrue(done.await(10, TimeUnit.SECONDS))
        pool.shutdown()

        val history = historyPort.findByContent(SubscriptionContentType.NEWSLETTER, "weekly")
        assertEquals(1, historyPort.count().toInt())
        assertEquals(emails.toSet(), mail.sentTo.toSet())
        assertEquals(SubscriptionDispatchStatus.SENT, history?.dispatchStatus)
        assertEquals(emails.size.toLong(), history?.successCount)
        assertEquals(0L, history?.failureCount)
    }

    @Test
    fun dispatchHistoryCreatedBeforeSending() {
        val historyPort = InMemorySubscriptionDispatchHistoryPort()
        val mail =
            RecordingOutboundMail { _ ->
                val claimed = historyPort.findByContent(SubscriptionContentType.NEWSLETTER, "weekly")
                assertEquals(SubscriptionDispatchStatus.SENDING, claimed?.dispatchStatus)
                assertTrue((claimed?.id ?: 0L) > 0L)
            }
        val service =
            mailService(
                emails = listOf("a@idolglow.app"),
                historyPort = historyPort,
                recipientPort = InMemorySubscriptionDispatchRecipientPort(),
                mail = mail,
            )

        service.dispatchNewsletter(newsletterEvent())

        val history = historyPort.findByContent(SubscriptionContentType.NEWSLETTER, "weekly")
        assertEquals(listOf("a@idolglow.app"), mail.sentTo)
        assertEquals(SubscriptionDispatchStatus.SENT, history?.dispatchStatus)
    }

    @Test
    fun resumeSendsOnlyToRemainingRecipients() {
        val historyPort = InMemorySubscriptionDispatchHistoryPort()
        val recipientPort = InMemorySubscriptionDispatchRecipientPort()
        val past = LocalDateTime.now().minusMinutes(SubscriptionDispatchHistory.RESUME_AFTER_MINUTES + 1)
        val claimed =
            historyPort.saveAndFlush(
                SubscriptionDispatchHistory.record(
                    contentType = SubscriptionContentType.NEWSLETTER,
                    contentSlug = "weekly",
                    contentTitle = "주간",
                    contentSummary = "요약",
                    recipientCount = 0,
                    contentCreatedAt = past,
                    dispatchedAt = past,
                    dispatchStatus = SubscriptionDispatchStatus.SENDING,
                ),
            )
        recipientPort.save(
            SubscriptionDispatchRecipient.create(claimed.id, "a@idolglow.app").also { recipient ->
                recipient.applyOutcome(sent = true, error = null, sentAt = past)
            },
        )
        val mail = RecordingOutboundMail()
        val service =
            mailService(
                emails = listOf("a@idolglow.app", "b@idolglow.app", "c@idolglow.app"),
                historyPort = historyPort,
                recipientPort = recipientPort,
                mail = mail,
            )

        service.dispatchNewsletter(newsletterEvent())

        val history = historyPort.findById(claimed.id)
        assertEquals(listOf("b@idolglow.app", "c@idolglow.app"), mail.sentTo)
        assertEquals(SubscriptionDispatchStatus.SENT, history?.dispatchStatus)
        assertEquals(3L, history?.successCount)
        assertEquals(0L, history?.failureCount)
    }

    @Test
    fun partialFailureRecorded() {
        val historyPort = InMemorySubscriptionDispatchHistoryPort()
        val recipientPort = InMemorySubscriptionDispatchRecipientPort()
        val mail = RecordingOutboundMail(failingRecipient = "b@idolglow.app")
        val service =
            mailService(
                emails = listOf("a@idolglow.app", "b@idolglow.app", "c@idolglow.app"),
                historyPort = historyPort,
                recipientPort = recipientPort,
                mail = mail,
            )

        service.dispatchNewsletter(newsletterEvent())

        val history = historyPort.findByContent(SubscriptionContentType.NEWSLETTER, "weekly")
        val failed =
            recipientPort
                .findByDispatchId(history?.id ?: 0L)
                .single { it.status == SubscriptionDispatchRecipientStatus.FAILED }
        assertEquals(SubscriptionDispatchStatus.PARTIAL_FAILED, history?.dispatchStatus)
        assertEquals(2L, history?.successCount)
        assertEquals(1L, history?.failureCount)
        assertEquals("b@idolglow.app", failed.email)
        assertTrue(failed.errorMessage?.contains("거절") == true)
        assertEquals(listOf("a@idolglow.app", "c@idolglow.app"), mail.sentTo)
    }

    private fun mailService(
        emails: List<String>,
        historyPort: SubscriptionDispatchHistoryPort,
        recipientPort: SubscriptionDispatchRecipientPort,
        mail: RecordingOutboundMail,
    ): SubscriptionDispatchMailService =
        SubscriptionDispatchMailService(
            appMailProperties = AppMailProperties(enabled = true),
            emailSubscriptionPort = FixedAudienceEmails(emails),
            outboundMailPort = mail,
            subscriptionEmailComposer =
                SubscriptionEmailComposer(
                    appMailProperties = AppMailProperties(enabled = true),
                    appPublicUrlProperties = AppPublicUrlProperties(publicBaseUrl = "https://idolglow.test"),
                ),
            subscriptionDispatchTxService =
                SubscriptionDispatchTxService(
                    subscriptionDispatchHistoryPort = historyPort,
                    subscriptionDispatchRecipientPort = recipientPort,
                ),
        )

    private fun newsletterEvent(): NewsletterDispatchRequestedEvent =
        NewsletterDispatchRequestedEvent(
            slug = "weekly",
            title = "주간",
            summary = "요약",
            imageUrl = "https://idolglow.test/cover.png",
            publishedAt = LocalDate.of(2026, 10, 25),
            tags = emptyList(),
            paragraphs = listOf("본문"),
            contentCreatedAt = LocalDateTime.of(2026, 10, 1, 0, 0),
        )
}

private class FixedAudienceEmails(
    private val emails: List<String>,
) : EmailSubscriptionPort {
    override fun findAllByLatest(): List<EmailSubscription> = emptyList()

    override fun findByEmail(email: String): EmailSubscription? = null

    override fun findActiveEmailsByAudience(audience: SubscriptionAudience): List<String> = emails

    override fun save(subscription: EmailSubscription): EmailSubscription = subscription

    override fun count(): Long = emails.size.toLong()

    override fun countActive(): Long = emails.size.toLong()

    override fun countActiveByAudience(audience: SubscriptionAudience): Long = emails.size.toLong()
}

private class RecordingOutboundMail(
    private val failingRecipient: String? = null,
    private val beforeSend: (OutboundMailMessage) -> Unit = {},
) : OutboundMailPort {
    val sentTo: MutableList<String> = mutableListOf()

    override fun send(message: OutboundMailMessage) {
        beforeSend(message)
        if (message.to == failingRecipient) {
            throw IllegalStateException("수신 서버가 거절했습니다.")
        }
        synchronized(sentTo) {
            sentTo.add(message.to)
        }
    }
}

private class InMemorySubscriptionDispatchHistoryPort : SubscriptionDispatchHistoryPort {
    var findGate: CountDownLatch? = null
    var saveGate: CountDownLatch? = null
    private val monitor = Any()
    private val nextId = AtomicLong(0)
    private val byKey = linkedMapOf<String, SubscriptionDispatchHistory>()
    private val byId = linkedMapOf<Long, SubscriptionDispatchHistory>()

    override fun findAllByLatest(): List<SubscriptionDispatchHistory> =
        synchronized(monitor) {
            byId.values.sortedByDescending { it.dispatchedAt }
        }

    override fun save(history: SubscriptionDispatchHistory): SubscriptionDispatchHistory = persist(history)

    override fun saveAndFlush(history: SubscriptionDispatchHistory): SubscriptionDispatchHistory {
        awaitGate(saveGate)
        return persist(history)
    }

    override fun count(): Long = synchronized(monitor) { byId.size.toLong() }

    override fun existsByContentTypeAndContentSlug(
        contentType: SubscriptionContentType,
        contentSlug: String,
    ): Boolean = findByContent(contentType, contentSlug) != null

    override fun findByContent(
        contentType: SubscriptionContentType,
        contentSlug: String,
    ): SubscriptionDispatchHistory? {
        awaitGate(findGate)
        return synchronized(monitor) { byKey["${contentType.name}|$contentSlug"] }
    }

    override fun findById(id: Long): SubscriptionDispatchHistory? = synchronized(monitor) { byId[id] }

    override fun claimResume(
        id: Long,
        now: LocalDateTime,
        staleBefore: LocalDateTime,
    ): Boolean {
        synchronized(monitor) {
            val history = byId[id]
            if (history == null || !history.isStaleSending(staleBefore)) {
                return false
            }
            history.beginAttempt(now)
            return true
        }
    }

    override fun findStaleSending(staleBefore: LocalDateTime): List<SubscriptionDispatchHistory> =
        synchronized(monitor) {
            byId.values.filter { it.isStaleSending(staleBefore) }
        }

    private fun persist(history: SubscriptionDispatchHistory): SubscriptionDispatchHistory =
        synchronized(monitor) {
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
            history
        }

    private fun awaitGate(gate: CountDownLatch?) {
        if (gate == null) {
            return
        }
        gate.countDown()
        if (!gate.await(5, TimeUnit.SECONDS)) {
            throw IllegalStateException("구독 발송 선점 대기가 끝나지 않았습니다.")
        }
    }
}

private class InMemorySubscriptionDispatchRecipientPort : SubscriptionDispatchRecipientPort {
    private val monitor = Any()
    private val nextId = AtomicLong(0)
    private val byKey = linkedMapOf<String, SubscriptionDispatchRecipient>()

    override fun save(recipient: SubscriptionDispatchRecipient): SubscriptionDispatchRecipient =
        synchronized(monitor) {
            val key = "${recipient.dispatchId}|${recipient.email}"
            val current = byKey[key]
            if (current != null && current !== recipient) {
                throw DataIntegrityViolationException("이미 같은 수신자 발송 결과가 있습니다.")
            }
            if (recipient.id == 0L) {
                val field = SubscriptionDispatchRecipient::class.java.getDeclaredField("id")
                field.isAccessible = true
                field.set(recipient, nextId.incrementAndGet())
            }
            byKey[key] = recipient
            recipient
        }

    override fun findByDispatchId(dispatchId: Long): List<SubscriptionDispatchRecipient> =
        synchronized(monitor) {
            byKey.values.filter { it.dispatchId == dispatchId }
        }

    override fun findByDispatchIdAndEmail(
        dispatchId: Long,
        email: String,
    ): SubscriptionDispatchRecipient? = synchronized(monitor) { byKey["$dispatchId|$email"] }
}
