package com.sleekydz86.idolglow.luckydraw.infrastructure

import com.sleekydz86.idolglow.luckydraw.domain.LuckyDraw
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDrawDraft
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDrawEntry
import com.sleekydz86.idolglow.support.MysqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("응모 행사 저장·조회 (MySQL)")
class LuckyDrawMysqlTest : MysqlIntegrationTest() {
    @Autowired private lateinit var luckyDrawJpaRepository: LuckyDrawJpaRepository

    @Autowired private lateinit var luckyDrawEntryJpaRepository: LuckyDrawEntryJpaRepository

    @Autowired private lateinit var transactionManager: PlatformTransactionManager

    @DisplayName("이모지·일본어·중국어 제목과 마이크로초 시각을 저장하고 다시 읽으면 같고, 응모를 회원별로 센다")
    @Test
    fun roundTrip() {
        // given
        val start = LocalDateTime.of(2031, 8, 1, 11, 0, 0, 123_456_000)
        val draft = LuckyDrawDraft("🎤응모 応募 抽奖", "경품", "설명", 3, start, start.plusDays(7), start.plusDays(7))
        val drawId = luckyDrawJpaRepository.save(LuckyDraw.create(draft)).id
        listOf(1, 2).forEach { day ->
            luckyDrawEntryJpaRepository.save(
                LuckyDrawEntry(
                    luckyDrawId = drawId,
                    userId = 7L,
                    entryDate = LocalDate.of(2031, 8, day),
                    enteredAt = start,
                ),
            )
        }

        // when
        var title = ""
        var startsAt = start
        TransactionTemplate(transactionManager).executeWithoutResult {
            val read = luckyDrawJpaRepository.findById(drawId).orElseThrow()
            title = read.title
            startsAt = read.entryStartsAt
        }
        val counts = luckyDrawEntryJpaRepository.countByUser(drawId).associate { (it[0] as Long) to (it[1] as Long) }

        // then
        assertEquals("🎤응모 応募 抽奖", title)
        assertEquals(start, startsAt)
        assertEquals(mapOf(7L to 2L), counts)
    }
}
