package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchHistoryPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchRecipientPort
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipient
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipientCounts
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSkipReason
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchStatus
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

@DisplayName("발송 기록별 수신자 상태 집계")
class SubscriptionDispatchRecipientCountQueryTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var historyPort: SubscriptionDispatchHistoryPort

    @Autowired
    private lateinit var recipientPort: SubscriptionDispatchRecipientPort

    @DisplayName("발송 기록마다 발송·실패·건너뜀 수를 한 번에 센다")
    @Test
    fun countsRecipientsPerDispatchAndStatus() {
        // given
        val now = LocalDateTime.of(2026, 11, 14, 9, 0)
        val partial = saveHistory(now)
        val skippedOnly = saveHistory(now)
        val empty = saveHistory(now)
        saveSent(partial.id, "sent-1@example.com", now)
        saveSent(partial.id, "sent-2@example.com", now)
        saveFailed(partial.id, "failed@example.com", now)
        saveSkipped(partial.id, "skipped@example.com")
        saveSkipped(skippedOnly.id, "skipped-1@example.com")
        saveSkipped(skippedOnly.id, "skipped-2@example.com")

        // when
        val counts = recipientPort.countByDispatchIds(listOf(partial.id, skippedOnly.id, empty.id))

        // then
        assertEquals(
            SubscriptionDispatchRecipientCounts(sentCount = 2, failedCount = 1, skippedCount = 1),
            counts[partial.id],
        )
        assertEquals(
            SubscriptionDispatchRecipientCounts(sentCount = 0, failedCount = 0, skippedCount = 2),
            counts[skippedOnly.id],
        )
        assertFalse(counts.containsKey(empty.id))
    }

    @DisplayName("조회할 발송 기록이 없으면 빈 결과를 돌려준다")
    @Test
    fun returnsEmptyWhenNoDispatchIds() {
        // given
        val dispatchIds = emptyList<Long>()

        // when
        val counts = recipientPort.countByDispatchIds(dispatchIds)

        // then
        assertTrue(counts.isEmpty())
    }

    private fun saveHistory(now: LocalDateTime): SubscriptionDispatchHistory =
        historyPort.saveAndFlush(
            SubscriptionDispatchHistory.record(
                contentType = SubscriptionContentType.NEWSLETTER,
                contentSlug = "recipient-count-${UUID.randomUUID()}",
                contentTitle = "수신자 집계",
                contentSummary = null,
                recipientCount = 0,
                contentCreatedAt = null,
                dispatchedAt = now,
                dispatchStatus = SubscriptionDispatchStatus.SENDING,
            ),
        )

    private fun saveSent(
        dispatchId: Long,
        email: String,
        now: LocalDateTime,
    ) {
        recipientPort.save(
            SubscriptionDispatchRecipient.create(dispatchId, email).also { it.applyOutcome(true, null, now) },
        )
    }

    private fun saveFailed(
        dispatchId: Long,
        email: String,
        now: LocalDateTime,
    ) {
        recipientPort.save(
            SubscriptionDispatchRecipient.create(dispatchId, email).also { it.applyOutcome(false, "smtp", now) },
        )
    }

    private fun saveSkipped(
        dispatchId: Long,
        email: String,
    ) {
        recipientPort.save(
            SubscriptionDispatchRecipient.create(dispatchId, email).also {
                it.skip(SubscriptionDispatchSkipReason.entries.first())
            },
        )
    }
}
