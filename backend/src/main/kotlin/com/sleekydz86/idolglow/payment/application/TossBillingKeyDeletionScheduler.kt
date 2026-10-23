package com.sleekydz86.idolglow.payment.application

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class TossBillingKeyDeletionScheduler(
    private val billingKeyDeletionService: BillingKeyDeletionService,
) {
    @Scheduled(fixedDelayString = "\${payment.toss.billing.deletion-retry-interval-ms:300000}")
    @SchedulerLock(
        name = "tossBillingKeyDeletion",
        lockAtLeastFor = "PT1M",
        lockAtMostFor = "PT4M",
    )
    fun retryPendingDeletions() {
        retryPendingDeletions(LocalDateTime.now())
    }

    fun retryPendingDeletions(now: LocalDateTime) {
        billingKeyDeletionService.retryDue(now)
    }
}
