package com.sleekydz86.idolglow.productpackage.reservation.domain

import com.sleekydz86.idolglow.global.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "reservation_slot_waitlist",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_rsw_user_slot", columnNames = ["user_id", "reservation_slot_id"])
    ]
)
class ReservationSlotWaitlistEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_slot_id", nullable = false)
    val reservationSlot: ReservationSlot,
) : BaseEntity()
