package com.sleekydz86.idolglow.luckydraw.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.luckydraw.LuckyDrawExceptionType
import com.sleekydz86.idolglow.luckydraw.application.dto.LuckyDrawMyStatusResponse
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDraw
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDrawEntry
import com.sleekydz86.idolglow.luckydraw.infrastructure.LuckyDrawEntryJpaRepository
import com.sleekydz86.idolglow.luckydraw.infrastructure.LuckyDrawJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LuckyDrawEntryService(
    private val luckyDrawJpaRepository: LuckyDrawJpaRepository,
    private val luckyDrawEntryJpaRepository: LuckyDrawEntryJpaRepository,
    private val luckyDrawTicketCalculator: LuckyDrawTicketCalculator,
    private val luckyDrawClock: LuckyDrawClock,
) {
    @Transactional
    fun enter(
        drawId: Long,
        userId: Long,
    ): LuckyDrawMyStatusResponse {
        val draw = find(drawId)
        val now = luckyDrawClock.now()
        draw.assertOpen(now)
        if (luckyDrawEntryJpaRepository.existsByLuckyDrawIdAndUserIdAndEntryDate(drawId, userId, now.toLocalDate())) {
            throw CustomException(LuckyDrawExceptionType.ALREADY_ENTERED)
        }
        try {
            luckyDrawEntryJpaRepository.saveAndFlush(
                LuckyDrawEntry(luckyDrawId = drawId, userId = userId, entryDate = now.toLocalDate(), enteredAt = now),
            )
        } catch (exception: DataIntegrityViolationException) {
            throw AlreadyEnteredException(exception)
        }
        return myStatus(draw, userId)
    }

    @Transactional(readOnly = true)
    fun myStatus(
        drawId: Long,
        userId: Long,
    ): LuckyDrawMyStatusResponse = myStatus(find(drawId), userId)

    private fun myStatus(
        draw: LuckyDraw,
        userId: Long,
    ): LuckyDrawMyStatusResponse {
        val row = luckyDrawTicketCalculator.calculateFor(draw, userId)
        return LuckyDrawMyStatusResponse(
            drawId = draw.id,
            enteredToday =
                luckyDrawEntryJpaRepository.existsByLuckyDrawIdAndUserIdAndEntryDate(
                    draw.id,
                    userId,
                    luckyDrawClock.today(),
                ),
            entryCount = row.entryCount,
            purchaseCount = row.purchaseCount,
            tickets = row.tickets,
        )
    }

    private fun find(drawId: Long): LuckyDraw =
        luckyDrawJpaRepository.findById(drawId).orElseThrow { CustomException(LuckyDrawExceptionType.NOT_FOUND) }

    private class AlreadyEnteredException(
        cause: Throwable,
    ) : CustomException(LuckyDrawExceptionType.ALREADY_ENTERED) {
        init {
            initCause(cause)
        }
    }
}
