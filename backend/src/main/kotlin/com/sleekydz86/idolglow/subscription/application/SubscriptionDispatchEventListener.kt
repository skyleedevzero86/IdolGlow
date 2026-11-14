package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.subscription.application.event.IdolGlowIssueDispatchRequestedEvent
import com.sleekydz86.idolglow.subscription.application.event.NewsletterDispatchRequestedEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SubscriptionDispatchEventListener(
    private val subscriptionDispatchMailService: SubscriptionDispatchMailService,
) {
    @Async("subscriptionDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onNewsletterDispatch(event: NewsletterDispatchRequestedEvent) {
        subscriptionDispatchMailService.dispatchNewsletter(event)
    }

    @Async("subscriptionDispatchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onIdolGlowIssueDispatch(event: IdolGlowIssueDispatchRequestedEvent) {
        subscriptionDispatchMailService.dispatchIdolGlowIssue(event)
    }
}
