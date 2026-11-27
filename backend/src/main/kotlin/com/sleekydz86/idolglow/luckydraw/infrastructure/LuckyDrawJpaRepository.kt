package com.sleekydz86.idolglow.luckydraw.infrastructure

import com.sleekydz86.idolglow.luckydraw.domain.LuckyDraw
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface LuckyDrawJpaRepository : JpaRepository<LuckyDraw, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from LuckyDraw d where d.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): LuckyDraw?

    @Query("select d.id from LuckyDraw d where d.drawnAt is null and d.drawAt <= :now order by d.drawAt asc, d.id asc")
    fun findDueIds(
        @Param("now") now: LocalDateTime,
    ): List<Long>

    @Query(
        """
        select count(d) from LuckyDraw d
        where d.entryStartsAt < :endsAt and :startsAt < d.entryEndsAt and d.id <> :excludeId
        """,
    )
    fun countOverlapping(
        @Param("startsAt") startsAt: LocalDateTime,
        @Param("endsAt") endsAt: LocalDateTime,
        @Param("excludeId") excludeId: Long,
    ): Long

    @Query(
        """
        select d from LuckyDraw d
        where d.entryStartsAt <= :now and d.entryEndsAt > :now and d.drawnAt is null
        order by d.entryStartsAt desc
        limit 1
        """,
    )
    fun findOpen(
        @Param("now") now: LocalDateTime,
    ): LuckyDraw?

    fun findFirstByEntryEndsAtLessThanEqualAndDrawnAtIsNullOrderByEntryEndsAtDesc(endsBefore: LocalDateTime): LuckyDraw?

    fun findFirstByEntryStartsAtGreaterThanOrderByEntryStartsAtAsc(startsAfter: LocalDateTime): LuckyDraw?

    fun findFirstByDrawnAtIsNotNullOrderByDrawnAtDesc(): LuckyDraw?

    fun findAllByOrderByEntryStartsAtDesc(pageable: Pageable): List<LuckyDraw>
}
