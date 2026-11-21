package com.sleekydz86.idolglow.eventinfo.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "consumed_event",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_consumed_event_consumer_event", columnNames = ["consumer_name", "event_id"]),
    ],
)
class ConsumedEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "consumer_name", nullable = false, length = 80)
    val consumerName: String,
    @Column(name = "event_id", nullable = false, length = 36)
    val eventId: String,
    @Column(name = "processed_at", nullable = false)
    val processedAt: LocalDateTime,
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var result: EventMetaApplyResult? = null,
) {
    fun markResult(applied: EventMetaApplyResult) {
        result = applied
    }
}
