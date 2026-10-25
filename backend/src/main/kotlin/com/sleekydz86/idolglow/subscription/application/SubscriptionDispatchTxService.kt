package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchHistoryPort
import com.sleekydz86.idolglow.subscription.application.port.out.SubscriptionDispatchRecipientPort
import com.sleekydz86.idolglow.subscription.domain.SubscriptionContentType
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchHistory
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipient
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchRecipientStatus
import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SubscriptionDispatchTxService(
    private val subscriptionDispatchHistoryPort: SubscriptionDispatchHistoryPort,
    private val subscriptionDispatchRecipientPort: SubscriptionDispatchRecipientPort,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun insertClaim(history: SubscriptionDispatchHistory): SubscriptionDispatchHistory =
        subscriptionDispatchHistoryPort.saveAndFlush(history)

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun findClaimed(
        contentType: SubscriptionContentType,
        contentSlug: String,
    ): SubscriptionDispatchHistory? = subscriptionDispatchHistoryPort.findByContent(contentType, contentSlug)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun claimResume(
        id: Long,
        now: LocalDateTime,
        staleBefore: LocalDateTime,
    ): Boolean = subscriptionDispatchHistoryPort.claimResume(id, now, staleBefore)

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun sentEmails(dispatchId: Long): Set<String> =
        subscriptionDispatchRecipientPort
            .findByDispatchId(dispatchId)
            .filter { it.status == SubscriptionDispatchRecipientStatus.SENT }
            .map { it.email }
            .toSet()

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordOutcome(
        dispatchId: Long,
        email: String,
        sent: Boolean,
        error: String?,
        sentAt: LocalDateTime,
    ) {
        val existing = subscriptionDispatchRecipientPort.findByDispatchIdAndEmail(dispatchId, email)
        val row = existing ?: SubscriptionDispatchRecipient.create(dispatchId, email)
        row.applyOutcome(sent, error, sentAt)
        subscriptionDispatchRecipientPort.save(row)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun finish(historyId: Long) {
        val history =
            subscriptionDispatchHistoryPort.findById(historyId)?.takeIf {
                it.dispatchStatus == SubscriptionDispatchStatus.SENDING
            } ?: return
        val rows = subscriptionDispatchRecipientPort.findByDispatchId(historyId)
        val success = rows.count { it.status == SubscriptionDispatchRecipientStatus.SENT }.toLong()
        val failure = rows.count { it.status == SubscriptionDispatchRecipientStatus.FAILED }.toLong()
        history.complete(
            status = outcome(success, failure),
            recipientCount = success + failure,
            successCount = success,
            failureCount = failure,
        )
        subscriptionDispatchHistoryPort.save(history)
    }

    private fun outcome(
        success: Long,
        failure: Long,
    ): SubscriptionDispatchStatus =
        when {
            failure == 0L -> SubscriptionDispatchStatus.SENT
            success == 0L -> SubscriptionDispatchStatus.FAILED
            else -> SubscriptionDispatchStatus.PARTIAL_FAILED
        }
}
