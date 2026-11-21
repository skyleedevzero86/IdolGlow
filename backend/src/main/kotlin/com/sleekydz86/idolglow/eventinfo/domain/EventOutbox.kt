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
    name = "event_outbox",
    uniqueConstraints = [UniqueConstraint(name = "uk_event_outbox_event_id", columnNames = ["event_id"])],
)
class EventOutbox(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "event_id", nullable = false, length = 36)
    val eventId: String,
    @Column(name = "aggregate_id", nullable = false, length = 36)
    val aggregateId: String,
    @Column(name = "event_type", nullable = false, length = 40)
    val eventType: String,
    @Column(name = "meta_version", nullable = false)
    val metaVersion: Long,
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false)
    val payload: String,
    @Column(name = "schema_version", nullable = false)
    val schemaVersion: Int,
    @Column(name = "sent_at")
    var sentAt: LocalDateTime? = null,
) : BaseEntity() {
    fun markSent(at: LocalDateTime) {
        sentAt = at
    }
}
