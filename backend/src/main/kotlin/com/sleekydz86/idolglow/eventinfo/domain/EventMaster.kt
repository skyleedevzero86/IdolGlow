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
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "event_master",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_event_master_global_id", columnNames = ["global_id"]),
    ],
)
class EventMaster(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "global_id", nullable = false, length = 36)
    val globalId: String,
    @Column(nullable = false, length = 255)
    var title: String,
    @Column(name = "normalized_title", nullable = false, length = 255)
    var normalizedTitle: String,
    @Column(name = "start_date")
    var startDate: LocalDate?,
    @Column(name = "end_date")
    var endDate: LocalDate?,
    @Column(length = 255)
    var venue: String?,
    @Column(name = "normalized_venue", length = 255)
    var normalizedVenue: String?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: EventMasterStatus,
) : BaseEntity() {
    fun refresh(item: CollectedPerformance) {
        val normalized = EventTitleNormalizer.normalize(item.title)
        require(normalized.isNotEmpty()) { "공연 제목을 정규화하면 비어 있습니다. title=${item.title}" }
        title = item.title.trim().take(TITLE_MAX)
        normalizedTitle = normalized.take(TITLE_MAX)
        startDate = item.startDate
        endDate = item.endDate
        venue = item.venue?.trim()?.take(TITLE_MAX)?.takeIf { it.isNotEmpty() }
        normalizedVenue = venue?.let(EventTitleNormalizer::normalize)?.takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val TITLE_MAX: Int = 255

        fun open(item: CollectedPerformance): EventMaster {
            val created =
                EventMaster(
                    globalId = UUID.randomUUID().toString(),
                    title = item.title,
                    normalizedTitle = "",
                    startDate = null,
                    endDate = null,
                    venue = null,
                    normalizedVenue = null,
                    status = EventMasterStatus.ACTIVE,
                )
            created.refresh(item)
            return created
        }
    }
}
