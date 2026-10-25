package com.sleekydz86.idolglow.eventinfo.application

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class EventMetaCollectScheduler(
    private val eventMetaCollectService: EventMetaCollectService,
) {
    @Scheduled(cron = "\${eventinfo.collect.cron:0 0 4 * * *}", zone = "Asia/Seoul")
    fun collectUpcomingPerformances() {
        val today = LocalDate.now()
        eventMetaCollectService.collect(today, today.plusDays(WINDOW_DAYS))
    }

    companion object {
        const val WINDOW_DAYS: Long = 30
    }
}
