package com.sleekydz86.idolglow.luckydraw.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "lucky_draw_winners",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_lucky_draw_winner", columnNames = ["lucky_draw_id", "user_id"]),
        UniqueConstraint(name = "uk_lucky_draw_winner_order", columnNames = ["lucky_draw_id", "winner_order"]),
    ],
)
class LuckyDrawWinner(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "lucky_draw_id", nullable = false)
    val luckyDrawId: Long,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "winner_order", nullable = false)
    val winnerOrder: Int,
) : BaseEntity()
