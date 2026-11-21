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
import java.time.OffsetDateTime

@Entity
@Table(
    name = "event_session",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_event_session_ref_starts", columnNames = ["source_ref_id", "starts_at"]),
    ],
)
class EventSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "source_ref_id", nullable = false)
    val sourceRefId: Long,
    @Column(name = "starts_at", nullable = false)
    val startsAt: OffsetDateTime,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: EventSessionStatus = EventSessionStatus.SCHEDULED,
) : BaseEntity() {
    fun cancel(): Boolean {
        if (status == EventSessionStatus.CANCELLED) return false
        status = EventSessionStatus.CANCELLED
        return true
    }

    fun reschedule(): Boolean {
        if (status == EventSessionStatus.SCHEDULED) return false
        status = EventSessionStatus.SCHEDULED
        return true
    }
}

enum class EventSessionStatus {
    SCHEDULED,
    CANCELLED,
}
