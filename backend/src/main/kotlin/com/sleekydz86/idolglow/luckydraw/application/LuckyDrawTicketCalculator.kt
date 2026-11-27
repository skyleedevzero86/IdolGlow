package com.sleekydz86.idolglow.luckydraw.application

import com.sleekydz86.idolglow.luckydraw.domain.LuckyDraw
import com.sleekydz86.idolglow.luckydraw.infrastructure.LuckyDrawEntryJpaRepository
import com.sleekydz86.idolglow.luckydraw.infrastructure.LuckyDrawPurchaseQuery
import org.springframework.stereotype.Component

data class LuckyDrawTicketRow(
    val userId: Long,
    val entryCount: Int,
    val purchaseCount: Int,
) {
    val tickets: Long = if (entryCount == 0) 0L else entryCount.toLong() + purchaseCount.toLong() * PURCHASE_BONUS

    companion object {
        const val PURCHASE_BONUS = 10L
    }
}

@Component
class LuckyDrawTicketCalculator(
    private val luckyDrawEntryJpaRepository: LuckyDrawEntryJpaRepository,
    private val luckyDrawPurchaseQuery: LuckyDrawPurchaseQuery,
) {
    fun calculate(draw: LuckyDraw): List<LuckyDrawTicketRow> {
        val entries =
            luckyDrawEntryJpaRepository
                .countByUser(draw.id)
                .associate { row -> (row[0] as Long) to (row[1] as Long).toInt() }
        val purchases = luckyDrawPurchaseQuery.countEventPurchases(entries.keys, draw.entryStartsAt, draw.entryEndsAt)
        return entries
            .map { (userId, entryCount) -> LuckyDrawTicketRow(userId, entryCount, purchases[userId] ?: 0) }
            .sortedBy { it.userId }
    }

    fun calculateFor(
        draw: LuckyDraw,
        userId: Long,
    ): LuckyDrawTicketRow {
        val entryCount = luckyDrawEntryJpaRepository.countByLuckyDrawIdAndUserId(draw.id, userId).toInt()
        val purchases = luckyDrawPurchaseQuery.countEventPurchases(listOf(userId), draw.entryStartsAt, draw.entryEndsAt)
        return LuckyDrawTicketRow(userId, entryCount, purchases[userId] ?: 0)
    }
}
