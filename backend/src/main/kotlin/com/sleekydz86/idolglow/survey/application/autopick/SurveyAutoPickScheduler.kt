package com.sleekydz86.idolglow.survey.application.autopick

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
@ConditionalOnProperty(
    prefix = "payment.toss.billing",
    name = ["scheduler-enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class SurveyAutoPickScheduler(
    private val txService: SurveyAutoPickTxService,
    private val chargeService: SurveyAutoPickChargeService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${survey.auto-pick.interval-ms:60000}")
    @SchedulerLock(name = "surveyAutoPickSchedule", lockAtLeastFor = "PT10S", lockAtMostFor = "PT5M")
    fun scheduleCandidates() {
        val created = txService.createCandidates(LocalDateTime.now())
        if (created > 0) {
            log.info("자동 담기 결제 예정 후보 생성: {}건", created)
        }
    }

    @Scheduled(fixedDelayString = "\${survey.auto-pick.interval-ms:60000}")
    @SchedulerLock(name = "surveyAutoPickCharge", lockAtLeastFor = "PT10S", lockAtMostFor = "PT10M")
    fun chargeCandidates() {
        val booked = chargeService.chargeDue()
        if (booked > 0) {
            log.info("자동 담기 자동 예약 완료: {}건", booked)
        }
    }
}
