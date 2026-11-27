package com.sleekydz86.idolglow.luckydraw.domain

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.luckydraw.LuckyDrawExceptionType
import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "lucky_draws",
    check = [
        CheckConstraint(
            name = "chk_lucky_draws_shape",
            constraint =
                "winner_count BETWEEN 1 AND 10 AND entry_starts_at < entry_ends_at AND entry_ends_at <= draw_at " +
                    "AND ((draw_seed IS NULL AND drawn_at IS NULL) " +
                    "OR (draw_seed IS NOT NULL AND drawn_at IS NOT NULL))",
        ),
    ],
)
class LuckyDraw(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    title: String,
    prizeName: String,
    prizeDescription: String,
    winnerCount: Int,
    entryStartsAt: LocalDateTime,
    entryEndsAt: LocalDateTime,
    drawAt: LocalDateTime,
) : BaseEntity() {
    @Column(name = "title", nullable = false, length = 100)
    var title: String = title
        protected set

    @Column(name = "prize_name", nullable = false, length = 100)
    var prizeName: String = prizeName
        protected set

    @Column(name = "prize_description", nullable = false, length = 1000)
    var prizeDescription: String = prizeDescription
        protected set

    @Column(name = "winner_count", nullable = false)
    var winnerCount: Int = winnerCount
        protected set

    @Column(name = "entry_starts_at", nullable = false)
    var entryStartsAt: LocalDateTime = entryStartsAt
        protected set

    @Column(name = "entry_ends_at", nullable = false)
    var entryEndsAt: LocalDateTime = entryEndsAt
        protected set

    @Column(name = "draw_at", nullable = false)
    var drawAt: LocalDateTime = drawAt
        protected set

    @Column(name = "draw_seed")
    var drawSeed: Long? = null
        protected set

    @Column(name = "drawn_at")
    var drawnAt: LocalDateTime? = null
        protected set

    fun status(now: LocalDateTime): LuckyDrawStatus =
        when {
            drawnAt != null -> LuckyDrawStatus.DRAWN
            now.isBefore(entryStartsAt) -> LuckyDrawStatus.SCHEDULED
            now.isBefore(entryEndsAt) -> LuckyDrawStatus.OPEN
            else -> LuckyDrawStatus.CLOSED
        }

    fun assertOpen(now: LocalDateTime) {
        if (status(now) != LuckyDrawStatus.OPEN) throw CustomException(LuckyDrawExceptionType.NOT_OPEN)
    }

    fun assertEditable(now: LocalDateTime) {
        if (!now.isBefore(entryStartsAt)) throw CustomException(LuckyDrawExceptionType.ALREADY_STARTED)
    }

    fun isDue(now: LocalDateTime): Boolean = drawnAt == null && !drawAt.isAfter(now)

    fun overlaps(other: LuckyDraw): Boolean =
        entryStartsAt.isBefore(other.entryEndsAt) && other.entryStartsAt.isBefore(entryEndsAt)

    fun update(
        draft: LuckyDrawDraft,
        now: LocalDateTime,
    ) {
        assertEditable(now)
        draft.validate()
        title = draft.title.trim()
        prizeName = draft.prizeName.trim()
        prizeDescription = draft.prizeDescription.trim()
        winnerCount = draft.winnerCount
        entryStartsAt = draft.entryStartsAt
        entryEndsAt = draft.entryEndsAt
        drawAt = draft.drawAt
    }

    fun completeDraw(
        seed: Long,
        now: LocalDateTime,
    ) {
        check(drawnAt == null) { "이미 추첨을 마친 추첨입니다. drawId=$id" }
        check(isDue(now)) { "추첨 시각이 지나지 않았습니다. drawId=$id" }
        drawSeed = seed
        drawnAt = now
    }

    companion object {
        fun create(draft: LuckyDrawDraft): LuckyDraw {
            draft.validate()
            return LuckyDraw(
                title = draft.title.trim(),
                prizeName = draft.prizeName.trim(),
                prizeDescription = draft.prizeDescription.trim(),
                winnerCount = draft.winnerCount,
                entryStartsAt = draft.entryStartsAt,
                entryEndsAt = draft.entryEndsAt,
                drawAt = draft.drawAt,
            )
        }
    }
}
