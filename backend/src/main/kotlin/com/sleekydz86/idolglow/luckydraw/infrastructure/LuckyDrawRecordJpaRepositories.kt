package com.sleekydz86.idolglow.luckydraw.infrastructure

import com.sleekydz86.idolglow.luckydraw.domain.LuckyDrawEntry
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDrawTicket
import com.sleekydz86.idolglow.luckydraw.domain.LuckyDrawWinner
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface LuckyDrawEntryJpaRepository : JpaRepository<LuckyDrawEntry, Long> {
    fun existsByLuckyDrawIdAndUserIdAndEntryDate(
        luckyDrawId: Long,
        userId: Long,
        entryDate: LocalDate,
    ): Boolean

    fun countByLuckyDrawIdAndUserId(
        luckyDrawId: Long,
        userId: Long,
    ): Long

    fun countByLuckyDrawId(luckyDrawId: Long): Long

    @Query("select e.userId, count(e) from LuckyDrawEntry e where e.luckyDrawId = :drawId group by e.userId")
    fun countByUser(
        @Param("drawId") drawId: Long,
    ): List<Array<Any>>
}

interface LuckyDrawTicketJpaRepository : JpaRepository<LuckyDrawTicket, Long> {
    fun findAllByLuckyDrawIdOrderByUserIdAsc(luckyDrawId: Long): List<LuckyDrawTicket>
}

interface LuckyDrawWinnerJpaRepository : JpaRepository<LuckyDrawWinner, Long> {
    fun findAllByLuckyDrawIdOrderByWinnerOrderAsc(luckyDrawId: Long): List<LuckyDrawWinner>
}
