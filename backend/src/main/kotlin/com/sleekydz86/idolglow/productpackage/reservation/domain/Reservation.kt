package com.sleekydz86.idolglow.productpackage.reservation.domain

import com.sleekydz86.idolglow.global.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "reservations")
class Reservation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_slot_id", nullable = false)
    var reservationSlot: ReservationSlot,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "visit_date", nullable = false)
    var visitDate: LocalDate,

    @Column(name = "visit_start_time", nullable = false)
    var visitStartTime: LocalTime,

    @Column(name = "visit_end_time", nullable = false)
    var visitEndTime: LocalTime,

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    var totalPrice: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReservationStatus = ReservationStatus.PREBOOK,

    @Column(name = "expires_at")
    var expiresAt: LocalDateTime? = null,

    @Column(name = "confirmed_at")
    var confirmedAt: LocalDateTime? = null,

    @Column(name = "canceled_at")
    var canceledAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason", length = 40)
    var cancelReason: ReservationCancelReason? = null,
) : BaseEntity() {

    init {
        alignWithSlot(reservationSlot)
    }

    private fun alignWithSlot(slot: ReservationSlot) {
        visitDate = slot.reservationDate
        visitStartTime = slot.startTime
        visitEndTime = slot.endTime
    }

    fun request(expiresAt: LocalDateTime): Reservation {
        changeStatus(ReservationStatus.PENDING)
        this.expiresAt = expiresAt
        confirmedAt = null
        canceledAt = null
        cancelReason = null
        return this
    }

    fun confirm(confirmedAt: LocalDateTime = LocalDateTime.now()): Reservation {
        changeStatus(ReservationStatus.BOOKED) {
            reservationSlot.confirmBooking(id, confirmedAt)
        }
        this.confirmedAt = confirmedAt
        this.cancelReason = null
        this.canceledAt = null
        return this
    }

    fun cancel(
        reason: ReservationCancelReason,
        canceledAt: LocalDateTime = LocalDateTime.now()
    ): Reservation {
        changeStatus(ReservationStatus.CANCELED) {
            when (status) {
                ReservationStatus.PENDING -> reservationSlot.releaseHold(id)
                ReservationStatus.BOOKED -> reservationSlot.cancelBooking()
                else -> Unit
            }
        }
        this.canceledAt = canceledAt
        this.cancelReason = reason
        return this
    }

    fun validateOwner(userId: Long) {
        require(this.userId == userId) { "Reservation can be handled only by its owner." }
    }

    fun isPending(): Boolean = status == ReservationStatus.PENDING

    fun isExpired(now: LocalDateTime = LocalDateTime.now()): Boolean =
        status == ReservationStatus.PENDING && expiresAt?.let { !it.isAfter(now) } == true

    fun reschedule(newSlot: ReservationSlot): Reservation {
        require(newSlot.product == reservationSlot.product) { "Reservation slot must belong to the same product." }
        require(!newSlot.isStatusBooked) { "Reservation slot is already booked." }
        ensureActive()
        reservationSlot = newSlot
        alignWithSlot(newSlot)
        return this
    }

    private fun ensureActive() {
        if (status == ReservationStatus.BOOKED || status == ReservationStatus.CANCELED) {
            throw IllegalStateException("Booked or canceled reservation cannot be modified.")
        }
    }

    fun resolveStatus(today: LocalDate = LocalDate.now()): ReservationStatus {
        return if (status == ReservationStatus.BOOKED && visitDate.isBefore(today)) {
            ReservationStatus.COMPLETED
        } else {
            status
        }
    }

    private fun changeStatus(targetStatus: ReservationStatus, beforeChange: (() -> Unit)? = null) {
        val allowed = when (status to targetStatus) {
            ReservationStatus.PREBOOK to ReservationStatus.PENDING -> true
            ReservationStatus.PENDING to ReservationStatus.BOOKED -> true
            ReservationStatus.PREBOOK to ReservationStatus.CANCELED -> true
            ReservationStatus.PENDING to ReservationStatus.CANCELED -> true
            ReservationStatus.BOOKED to ReservationStatus.CANCELED -> true
            else -> false
        }

        if (!allowed) {
            throw IllegalStateException("Cannot change status from $status to $targetStatus.")
        }

        beforeChange?.invoke()
        status = targetStatus
    }
}
