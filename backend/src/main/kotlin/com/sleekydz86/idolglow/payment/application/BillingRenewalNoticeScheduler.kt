package com.sleekydz86.idolglow.payment.application

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "payment.toss.billing",
    name = ["scheduler-enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class BillingRenewalNoticeScheduler(
    private val renewalNoticeService: BillingRenewalNoticeService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${payment.toss.billing.renewal-notice-interval-ms:3600000}")
    @SchedulerLock(
        name = "tossBillingRenewalNotice",
        lockAtLeastFor = "PT30S",
        lockAtMostFor = "PT10M",
    )
    fun sendRenewalNotices() {
        val sent = renewalNoticeService.sendDueNotices()
        if (sent > 0) {
            log.info("정기결제 갱신 안내 메일 발송: {}건", sent)
        }
    }
}
