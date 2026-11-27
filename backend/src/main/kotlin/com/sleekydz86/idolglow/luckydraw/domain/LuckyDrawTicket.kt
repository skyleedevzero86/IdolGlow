package com.sleekydz86.idolglow.luckydraw.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "lucky_draw_tickets",
    uniqueConstraints = [UniqueConstraint(name = "uk_lucky_draw_ticket", columnNames = ["lucky_draw_id", "user_id"])],
    check = [
        CheckConstraint(
            name = "chk_lucky_draw_tickets_non_negative",
            constraint = "entry_count >= 0 AND purchase_count >= 0 AND tickets >= 0",
        ),
    ],
)
class LuckyDrawTicket(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "lucky_draw_id", nullable = false)
    val luckyDrawId: Long,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "entry_count", nullable = false)
    val entryCount: Int,
    @Column(name = "purchase_count", nullable = false)
    val purchaseCount: Int,
    @Column(name = "tickets", nullable = false)
    val tickets: Long,
) : BaseEntity()
