package com.sleekydz86.idolglow.eventinfo.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "event_source_ref",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_event_source_ref_source", columnNames = ["source", "source_id"]),
    ],
)
class EventSourceRef(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(nullable = false, length = 80)
    val source: String,
    @Column(name = "source_id", nullable = false, length = 80)
    val sourceId: String,
    @Column(name = "global_id", length = 36)
    var globalId: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 20)
    var matchStatus: EventMatchStatus,
    @Column(name = "match_score", nullable = false)
    var matchScore: Int,
    @Column(name = "matched_at")
    var matchedAt: LocalDateTime? = null,
    @Column(name = "raw_hash", nullable = false, length = 64)
    var rawHash: String,
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "raw_payload")
    var rawPayload: String? = null,
    @Column(nullable = false, length = 255)
    var title: String,
    @Column(name = "start_date")
    var startDate: LocalDate? = null,
    @Column(name = "end_date")
    var endDate: LocalDate? = null,
    @Column(length = 255)
    var venue: String? = null,
) : BaseEntity() {
    fun assignMatch(
        globalId: String?,
        status: EventMatchStatus,
        score: Int,
        matchedAt: LocalDateTime?,
    ) {
        this.globalId = globalId
        matchStatus = status
        matchScore = score
        this.matchedAt = matchedAt
    }

    fun record(
        globalId: String?,
        status: EventMatchStatus,
        score: Int,
        item: CollectedPerformance,
        hash: String,
        matchedAt: LocalDateTime,
    ) {
        this.globalId = globalId
        matchStatus = status
        matchScore = score
        this.matchedAt = matchedAt
        rawHash = hash
        rawPayload = item.rawPayload
        title = item.title.trim().take(TITLE_MAX)
        startDate = item.startDate
        endDate = item.endDate
        venue = item.venue?.trim()?.take(TITLE_MAX)?.takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val TITLE_MAX: Int = 255

        fun create(
            source: String,
            sourceId: String,
        ): EventSourceRef {
            require(source.isNotBlank()) { "공연 공급처는 비울 수 없습니다." }
            require(sourceId.isNotBlank()) { "공연 공급처 ID는 비울 수 없습니다." }
            return EventSourceRef(
                source = source.trim().take(SOURCE_MAX),
                sourceId = sourceId.trim().take(SOURCE_MAX),
                matchStatus = EventMatchStatus.CONFLICT,
                matchScore = 0,
                rawHash = "",
                title = sourceId,
            )
        }

        private const val SOURCE_MAX: Int = 80
    }
}
