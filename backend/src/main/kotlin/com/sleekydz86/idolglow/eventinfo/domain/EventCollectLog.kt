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
import java.time.LocalDateTime

@Entity
@Table(name = "event_collect_log")
class EventCollectLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(nullable = false, length = 80)
    val source: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: EventCollectStatus,
    @Column(name = "item_count", nullable = false)
    val itemCount: Int,
    @Column(name = "error_message", length = 1000)
    val errorMessage: String?,
    @Column(name = "collected_at", nullable = false)
    val collectedAt: LocalDateTime,
) : BaseEntity() {
    companion object {
        private const val SOURCE_MAX: Int = 80
        private const val MESSAGE_MAX: Int = 1000

        fun record(
            source: String,
            status: EventCollectStatus,
            itemCount: Int,
            errorMessage: String?,
            collectedAt: LocalDateTime,
        ): EventCollectLog {
            require(source.isNotBlank()) { "공연 공급처는 비울 수 없습니다." }
            require(itemCount >= 0) { "수집 건수는 0 이상이어야 합니다. itemCount=$itemCount" }
            return EventCollectLog(
                source = source.trim().take(SOURCE_MAX),
                status = status,
                itemCount = itemCount,
                errorMessage = errorMessage?.trim()?.take(MESSAGE_MAX)?.takeIf { it.isNotEmpty() },
                collectedAt = collectedAt,
            )
        }
    }
}
