package com.sleekydz86.idolglow.schedule.domain

import com.sleekydz86.idolglow.global.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "schedules",
    indexes = [
        Index(name = "idx_schedule_user_start", columnList = "user_id,start_at")
    ]
)
class Schedule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(nullable = false, length = 100)
    var title: String,

    @Column(name = "start_at", nullable = false)
    var startAt: LocalDateTime,

    @Column(name = "end_at", nullable = false)
    var endAt: LocalDateTime,
) : BaseEntity() {

    init {
        validate(title, startAt, endAt, productId)
        title = title.trim()
    }

    private fun validate(title: String, startAt: LocalDateTime, endAt: LocalDateTime, productId: Long) {
        require(title.isNotBlank()) { "Schedule title must not be blank." }
        require(productId > 0) { "Schedule productId must be greater than zero." }
        require(!endAt.isBefore(startAt)) { "Schedule end time must be after start time." }
    }

    fun update(title: String, startAt: LocalDateTime, endAt: LocalDateTime) {
        validate(title, startAt, endAt, productId)
        this.title = title.trim()
        this.startAt = startAt
        this.endAt = endAt
    }

    companion object {
        fun of(
            userId: Long,
            productId: Long,
            title: String,
            startAt: LocalDateTime,
            endAt: LocalDateTime
        ): Schedule = Schedule(
            userId = userId,
            productId = productId,
            title = title.trim(),
            startAt = startAt,
            endAt = endAt
        )
    }
}