package com.sleekydz86.idolglow.eventinfo.application

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class EventMetaCollectScheduler(
    private val eventMetaCollectService: EventMetaCollectService,
    private val tourEventTranslationCollector: TourEventTranslationCollector,
) {
    @Scheduled(cron = "\${eventinfo.collect.cron:0 0 4 * * *}", zone = "Asia/Seoul")
    fun collectUpcomingPerformances() {
        val today = LocalDate.now()
        val to = today.plusDays(WINDOW_DAYS)
        eventMetaCollectService.collect(today, to)
        tourEventTranslationCollector.collect(today, to)
    }

    companion object {
        const val WINDOW_DAYS: Long = 30
    }
}
