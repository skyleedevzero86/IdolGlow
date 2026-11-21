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
import java.time.LocalDateTime
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
    @Column(name = "merged_into", length = 36)
    var mergedInto: String? = null,
    @Column(name = "meta_version", nullable = false)
    var metaVersion: Long = 0,
) : BaseEntity() {
    fun nextMetaVersion(): Long {
        metaVersion += 1
        return metaVersion
    }

    fun snapshot(): EventMetaSnapshot = EventMetaSnapshot(title, startDate, endDate, venue, status.name, mergedInto)

    fun mergeInto(target: EventMaster) {
        require(target.globalId != globalId) { "같은 글로벌 공연끼리는 병합할 수 없습니다. globalId=$globalId" }
        check(status == EventMasterStatus.ACTIVE) { "이미 병합된 글로벌 공연입니다. globalId=$globalId" }
        check(target.status == EventMasterStatus.ACTIVE) {
            "병합 대상이 이미 병합된 글로벌 공연입니다. globalId=${target.globalId}"
        }
        status = EventMasterStatus.MERGED
        mergedInto = target.globalId
    }

    fun markTranslationChanged(changedAt: LocalDateTime) {
        updatedAt = changedAt
    }

    fun markExternalIdChanged(changedAt: LocalDateTime) {
        updatedAt = changedAt
    }

    fun markSessionsChanged(changedAt: LocalDateTime) {
        updatedAt = changedAt
    }

    fun restoreActive() {
        status = EventMasterStatus.ACTIVE
        mergedInto = null
    }

    fun refresh(item: CollectedPerformance): Set<String> {
        val before = snapshot()
        val normalized = EventTitleNormalizer.normalize(item.title)
        require(normalized.isNotEmpty()) { "공연 제목을 정규화하면 비어 있습니다. title=${item.title}" }
        title = item.title.trim().take(TITLE_MAX)
        normalizedTitle = normalized.take(TITLE_MAX)
        startDate = item.startDate
        endDate = item.endDate
        venue = item.venue?.trim()?.take(TITLE_MAX)?.takeIf { it.isNotEmpty() }
        normalizedVenue = venue?.let(EventTitleNormalizer::normalize)?.takeIf { it.isNotEmpty() }
        return before.changedFields(snapshot())
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
