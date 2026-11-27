package com.sleekydz86.idolglow.luckydraw.application

import com.sleekydz86.idolglow.luckydraw.infrastructure.LuckyDrawJpaRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class LuckyDrawScheduler(
    private val luckyDrawJpaRepository: LuckyDrawJpaRepository,
    private val luckyDrawRunTxService: LuckyDrawRunTxService,
    private val luckyDrawClock: LuckyDrawClock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${app.lucky-draw.run-interval-ms:60000}")
    @SchedulerLock(name = "luckyDrawRunner", lockAtMostFor = "PT5M", lockAtLeastFor = "PT10S")
    fun runDueDraws() {
        luckyDrawJpaRepository.findDueIds(luckyDrawClock.now()).forEach { drawId ->
            runCatching { luckyDrawRunTxService.run(drawId) }
                .onSuccess { drawn -> if (drawn) log.info("응모 추첨 완료 drawId={}", drawId) }
                .onFailure { log.error("응모 추첨 실패 drawId={}", drawId, it) }
        }
    }
}
