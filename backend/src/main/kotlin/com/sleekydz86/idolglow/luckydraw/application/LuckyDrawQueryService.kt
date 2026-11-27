package com.sleekydz86.idolglow.luckydraw.application

import com.sleekydz86.idolglow.luckydraw.application.dto.LuckyDrawSummaryResponse
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDraw
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDrawStatus
import com.sleekydz86.idolglow.luckydraw.infrastructure.LuckyDrawJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class LuckyDrawQueryService(
    private val luckyDrawJpaRepository: LuckyDrawJpaRepository,
    private val luckyDrawWinnerReader: LuckyDrawWinnerReader,
    private val luckyDrawClock: LuckyDrawClock,
) {
    fun current(): LuckyDrawSummaryResponse? {
        val now = luckyDrawClock.now()
        val draw = pickCurrent(now) ?: return null
        val drawn = draw.status(now) == LuckyDrawStatus.DRAWN
        val winners = if (drawn) luckyDrawWinnerReader.maskedWinners(draw.id) else emptyList()
        return LuckyDrawSummaryResponse.of(draw, now, winners)
    }

    private fun pickCurrent(now: LocalDateTime): LuckyDraw? =
        luckyDrawJpaRepository.findOpen(now)
            ?: luckyDrawJpaRepository.findFirstByEntryEndsAtLessThanEqualAndDrawnAtIsNullOrderByEntryEndsAtDesc(now)
            ?: luckyDrawJpaRepository.findFirstByEntryStartsAtGreaterThanOrderByEntryStartsAtAsc(now)
            ?: luckyDrawJpaRepository.findFirstByDrawnAtIsNotNullOrderByDrawnAtDesc()
}
