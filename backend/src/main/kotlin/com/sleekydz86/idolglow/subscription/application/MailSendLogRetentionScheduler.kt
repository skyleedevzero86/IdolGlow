package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

@Component
class MailSendLogRetentionScheduler(
    private val mailSendLogTxService: MailSendLogTxService,
    @param:Value("\${subscription.mail-log.retention-days:90}")
    private val retentionDays: Long,
) {
    private val log = LoggerFactory.getLogger(MailSendLogRetentionScheduler::class.java)
    internal var clock: Clock = Clock.system(IdolGlowDateTimes.ZONE)

    @Scheduled(
        cron = "\${subscription.mail-log.retention-cron:0 30 3 * * *}",
        zone = "Asia/Seoul",
    )
    @SchedulerLock(
        name = "mailSendLogRetention",
        lockAtLeastFor = "PT30S",
        lockAtMostFor = "PT30M",
    )
    fun purgeExpired() {
        require(retentionDays > 0) { "메일 발송 기록 보관 일수는 0보다 커야 합니다. retentionDays=$retentionDays" }
        val before = LocalDateTime.now(clock).minusDays(retentionDays)
        val deleted = mailSendLogTxService.deleteCreatedBefore(before)
        if (deleted > 0) {
            log.info("보관 기간이 지난 메일 발송 기록 {}건을 지웠습니다. 기준={}", deleted, before)
        }
    }
}
