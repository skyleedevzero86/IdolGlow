package com.sleekydz86.idolglow.luckydraw.application

import com.sleekydz86.idolglow.luckydraw.application.event.LuckyDrawCompletedEvent
import com.sleekydz86.idolglow.luckydraw.domain.DrawCandidate
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDraw
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDrawTicket
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDrawWinner
import com.sleekydz86.idolglow.luckydraw.domain.WeightedDrawer
import com.sleekydz86.idolglow.luckydraw.infrastructure.LuckyDrawJpaRepository
import com.sleekydz86.idolglow.luckydraw.infrastructure.LuckyDrawTicketJpaRepository
import com.sleekydz86.idolglow.luckydraw.infrastructure.LuckyDrawWinnerJpaRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LuckyDrawRunTxService(
    private val luckyDrawJpaRepository: LuckyDrawJpaRepository,
    private val luckyDrawTicketJpaRepository: LuckyDrawTicketJpaRepository,
    private val luckyDrawWinnerJpaRepository: LuckyDrawWinnerJpaRepository,
    private val luckyDrawTicketCalculator: LuckyDrawTicketCalculator,
    private val luckyDrawSeedSource: LuckyDrawSeedSource,
    private val luckyDrawClock: LuckyDrawClock,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun run(drawId: Long): Boolean {
        val draw = luckyDrawJpaRepository.findByIdForUpdate(drawId)
        val now = luckyDrawClock.now()
        if (draw == null || !draw.isDue(now)) return false
        val rows = luckyDrawTicketCalculator.calculate(draw).filter { it.tickets > 0 }
        val seed = luckyDrawSeedSource.nextSeed()
        val winners = WeightedDrawer.draw(seed, rows.map { DrawCandidate(it.userId, it.tickets) }, draw.winnerCount)
        luckyDrawTicketJpaRepository.saveAll(rows.map { toTicket(draw, it) })
        luckyDrawWinnerJpaRepository.saveAll(
            winners.mapIndexed { index, userId ->
                LuckyDrawWinner(luckyDrawId = draw.id, userId = userId, winnerOrder = index + 1)
            },
        )
        draw.completeDraw(seed, now)
        applicationEventPublisher.publishEvent(LuckyDrawCompletedEvent(draw.id, draw.title, draw.prizeName, winners))
        return true
    }

    private fun toTicket(
        draw: LuckyDraw,
        row: LuckyDrawTicketRow,
    ): LuckyDrawTicket =
        LuckyDrawTicket(
            luckyDrawId = draw.id,
            userId = row.userId,
            entryCount = row.entryCount,
            purchaseCount = row.purchaseCount,
            tickets = row.tickets,
        )
}
