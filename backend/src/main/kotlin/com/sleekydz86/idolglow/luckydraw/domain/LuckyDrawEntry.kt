package com.sleekydz86.idolglow.luckydraw.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "lucky_draw_entries",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_lucky_draw_entry_daily", columnNames = ["lucky_draw_id", "user_id", "entry_date"]),
    ],
)
class LuckyDrawEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "lucky_draw_id", nullable = false)
    val luckyDrawId: Long,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "entry_date", nullable = false)
    val entryDate: LocalDate,
    @Column(name = "entered_at", nullable = false)
    val enteredAt: LocalDateTime,
) : BaseEntity()
