package com.sleekydz86.idolglow.luckydraw.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.luckydraw.LuckyDrawExceptionType
import com.sleekydz86.idolglow.luckydraw.application.dto.LuckyDrawAdminResponse
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDraw
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDrawDraft
import com.sleekydz86.idolglow.luckydraw.infrastructure.LuckyDrawEntryJpaRepository
import com.sleekydz86.idolglow.luckydraw.infrastructure.LuckyDrawJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LuckyDrawAdminService(
    private val luckyDrawJpaRepository: LuckyDrawJpaRepository,
    private val luckyDrawEntryJpaRepository: LuckyDrawEntryJpaRepository,
    private val luckyDrawTicketCalculator: LuckyDrawTicketCalculator,
    private val luckyDrawWinnerReader: LuckyDrawWinnerReader,
    private val luckyDrawClock: LuckyDrawClock,
) {
    fun create(draft: LuckyDrawDraft): LuckyDrawAdminResponse {
        draft.validate()
        assertNoOverlap(draft, NEW_DRAW_ID)
        return toResponse(luckyDrawJpaRepository.save(LuckyDraw.create(draft)))
    }

    fun update(
        drawId: Long,
        draft: LuckyDrawDraft,
    ): LuckyDrawAdminResponse {
        val draw = find(drawId)
        draw.assertEditable(luckyDrawClock.now())
        draft.validate()
        assertNoOverlap(draft, drawId)
        draw.update(draft, luckyDrawClock.now())
        return toResponse(draw)
    }

    fun delete(drawId: Long) {
        val draw = find(drawId)
        draw.assertEditable(luckyDrawClock.now())
        luckyDrawJpaRepository.delete(draw)
    }

    @Transactional(readOnly = true)
    fun list(
        page: Int,
        size: Int,
    ): List<LuckyDrawAdminResponse> {
        require(size in 1..MAX_PAGE_SIZE) { "조회 개수는 1 이상 50 이하여야 합니다." }
        require(page >= 0) { "페이지는 0 이상이어야 합니다." }
        return luckyDrawJpaRepository.findAllByOrderByEntryStartsAtDesc(PageRequest.of(page, size)).map(::toResponse)
    }

    @Transactional(readOnly = true)
    fun detail(drawId: Long): LuckyDrawAdminResponse = toResponse(find(drawId))

    private fun find(drawId: Long): LuckyDraw =
        luckyDrawJpaRepository.findById(drawId).orElseThrow { CustomException(LuckyDrawExceptionType.NOT_FOUND) }

    private fun assertNoOverlap(
        draft: LuckyDrawDraft,
        excludeId: Long,
    ) {
        if (luckyDrawJpaRepository.countOverlapping(draft.entryStartsAt, draft.entryEndsAt, excludeId) > 0) {
            throw CustomException(LuckyDrawExceptionType.PERIOD_OVERLAP)
        }
    }

    private fun toResponse(draw: LuckyDraw): LuckyDrawAdminResponse {
        val rows = luckyDrawTicketCalculator.calculate(draw)
        return LuckyDrawAdminResponse(
            id = draw.id,
            title = draw.title,
            prizeName = draw.prizeName,
            prizeDescription = draw.prizeDescription,
            winnerCount = draw.winnerCount,
            entryStartsAt = draw.entryStartsAt,
            entryEndsAt = draw.entryEndsAt,
            drawAt = draw.drawAt,
            status = draw.status(luckyDrawClock.now()),
            drawnAt = draw.drawnAt,
            drawSeed = draw.drawSeed,
            entrantCount = rows.size,
            entryCount = luckyDrawEntryJpaRepository.countByLuckyDrawId(draw.id),
            purchaseCount = rows.sumOf { it.purchaseCount },
            winners = luckyDrawWinnerReader.adminWinners(draw.id),
        )
    }

    companion object {
        private const val NEW_DRAW_ID = 0L
        private const val MAX_PAGE_SIZE = 50
    }
}
