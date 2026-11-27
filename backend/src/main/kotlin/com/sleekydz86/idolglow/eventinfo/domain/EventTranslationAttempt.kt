package com.sleekydz86.idolglow.eventinfo.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "event_translation_attempt",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_event_translation_attempt_global_locale", columnNames = ["global_id", "locale"]),
    ],
)
class EventTranslationAttempt(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "global_id", nullable = false, length = 36)
    val globalId: String,
    @Column(nullable = false, length = 8)
    val locale: String,
    lastReason: String,
    lastFailedAt: LocalDateTime,
) : BaseEntity() {
    @Column(name = "fail_count", nullable = false)
    var failCount: Int = 1
        protected set

    @Column(name = "last_reason", nullable = false, length = REASON_MAX)
    var lastReason: String = limitReason(lastReason)
        protected set

    @Column(name = "last_failed_at", nullable = false)
    var lastFailedAt: LocalDateTime = lastFailedAt
        protected set

    fun recordFailure(
        reason: String,
        at: LocalDateTime,
    ) {
        failCount += 1
        lastReason = limitReason(reason)
        lastFailedAt = at
    }

    companion object {
        const val REASON_MAX: Int = 200

        fun first(
            globalId: String,
            locale: String,
            reason: String,
            at: LocalDateTime,
        ): EventTranslationAttempt =
            EventTranslationAttempt(
                globalId = globalId,
                locale = locale,
                lastReason = reason,
                lastFailedAt = at,
            )

        fun limitReason(reason: String): String {
            val count = reason.codePointCount(0, reason.length)
            if (count <= REASON_MAX) return reason
            return reason.substring(0, reason.offsetByCodePoints(0, REASON_MAX))
        }
    }
}
