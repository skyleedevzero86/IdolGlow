package com.sleekydz86.idolglow.payment.application

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
@ConditionalOnProperty(prefix = "payment.toss.billing", name = ["scheduler-enabled"], havingValue = "true", matchIfMissing = false)
class TossBillingScheduler(
    private val tossBillingService: TossBillingService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${payment.toss.billing.scheduler-interval-ms:60000}")
    @SchedulerLock(
        name = "tossBillingChargeDue",
        lockAtLeastFor = "PT30S",
        lockAtMostFor = "PT5M",
    )
    fun chargeDueSubscriptions() {
        val now = LocalDateTime.now()
        val ids = tossBillingService.findDueSubscriptionIds(now)
        var success = 0
        ids.forEach { id ->
            runCatching {
                if (tossBillingService.chargeOne(id)) {
                    success += 1
                }
            }.onFailure { error ->
                log.warn("구독 {} 자동결제 실패", id, error)
            }
        }
        if (success > 0) {
            log.info("토스 빌링 자동결제 처리 완료: {}건", success)
        }
    }
}
