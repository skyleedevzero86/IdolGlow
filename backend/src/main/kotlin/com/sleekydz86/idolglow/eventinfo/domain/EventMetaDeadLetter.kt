package com.sleekydz86.idolglow.eventinfo.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(
    name = "event_meta_dead_letter",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_event_meta_dead_letter_consumer_event",
            columnNames = ["consumer_name", "event_id"],
        ),
    ],
)
class EventMetaDeadLetter(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "consumer_name", nullable = false, length = 80)
    val consumerName: String,
    @Column(name = "event_id", nullable = false, length = 80)
    val eventId: String,
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false)
    val payload: String,
    @Column(name = "error_type", length = 255)
    val errorType: String?,
    @Column(name = "failed_at", nullable = false)
    val failedAt: LocalDateTime,
    @Column(name = "reprocessed_at")
    var reprocessedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun isReprocessed(): Boolean = reprocessedAt != null

    fun markReprocessed(at: LocalDateTime) {
        check(reprocessedAt == null) { "이미 다시 처리한 실패 메시지입니다. id=$id" }
        reprocessedAt = at
    }
}
