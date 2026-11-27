package com.sleekydz86.idolglow.eventinfo.application

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class EventMetaCollectScheduler(
    private val eventMetaCollectService: EventMetaCollectService,
    private val tourEventTranslationCollector: TourEventTranslationCollector,
    private val eventSessionSyncService: EventSessionSyncService,
    private val eventMachineTitleService: EventMachineTitleService,
) {
    @Scheduled(cron = "\${eventinfo.collect.cron:0 0 4 * * *}", zone = "Asia/Seoul")
    @SchedulerLock(name = "eventMetaCollect", lockAtMostFor = "PT2H")
    fun collectUpcomingPerformances() {
        val today = LocalDate.now()
        val to = today.plusDays(WINDOW_DAYS)
        eventMetaCollectService.collect(today, to)
        eventSessionSyncService.sync(today)
        tourEventTranslationCollector.collect(today, to)
        eventMachineTitleService.fill(today, to)
    }

    companion object {
        const val WINDOW_DAYS: Long = 30
    }
}
