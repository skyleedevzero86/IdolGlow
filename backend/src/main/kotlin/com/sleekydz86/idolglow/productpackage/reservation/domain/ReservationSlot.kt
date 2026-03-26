package com.sleekydz86.idolglow.productpackage.reservation.domain

import com.sleekydz86.idolglow.global.BaseEntity
import com.sleekydz86.idolglow.productpackage.product.domain.Product
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
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(
    name = "reservation_slots",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_reservation_slot_product_date_start",
            columnNames = ["product_id", "reservation_date", "start_time"]
        )
    ]
)
class ReservationSlot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,

    @Column(name = "reservation_date", nullable = false)
    val reservationDate: LocalDate,

    @Column(name = "start_time", nullable = false)
    val startTime: LocalTime,

    @Column(name = "end_time", nullable = false)
    val endTime: LocalTime
) : BaseEntity() {

    @Column(name = "is_booked", nullable = false)
    var isStatusBooked: Boolean = false
        private set

    @Column(name = "hold_reservation_id")
    var holdReservationId: Long? = null
        private set

    @Column(name = "hold_expires_at")
    var holdExpiresAt: LocalDateTime? = null
        private set

    init {
        val duration = Duration.between(startTime, endTime)
        require(startTime.minute == 0 && endTime.minute == 0) { "Reservation slots must start and end on the hour." }
        require(startTime.hour in 9..15) { "Reservation slots must start between 09:00 and 15:00." }
        require(endTime == startTime.plusHours(1)) { "Reservation slots must be exactly one hour long." }
        require(!duration.isZero && !duration.isNegative) { "Reservation slot end time must be after start time." }
    }

    fun validateAvailability(productId: Long, now: LocalDateTime = LocalDateTime.now()) {
        clearExpiredHold(now)
        require(!isStatusBooked) { "Reservation slot is already booked." }
        require(this.product.id == productId) { "Slot does not belong to product $productId." }
        require(!isHeld(now)) { "Reservation slot is already held by another reservation." }
    }

    fun hold(reservationId: Long, expiresAt: LocalDateTime, now: LocalDateTime = LocalDateTime.now()) {
        clearExpiredHold(now)
        if (isStatusBooked) {
            throw IllegalStateException("Reservation slot is already booked.")
        }
        if (isHeld(now) && holdReservationId != reservationId) {
            throw IllegalStateException("Reservation slot is already held.")
        }
        holdReservationId = reservationId
        holdExpiresAt = expiresAt
    }

    fun confirmBooking(reservationId: Long, now: LocalDateTime = LocalDateTime.now()) {
        clearExpiredHold(now)
        require(!isStatusBooked) { "Reservation slot is already booked." }
        require(holdReservationId == reservationId) { "Reservation slot is not held by this reservation." }
        require(holdExpiresAt?.isAfter(now) == true) { "Reservation hold already expired." }
        isStatusBooked = true
        clearHold()
    }

    fun releaseHold(reservationId: Long) {
        if (holdReservationId == null) {
            return
        }
        require(holdReservationId == reservationId) { "Reservation slot hold belongs to another reservation." }
        clearHold()
    }

    fun cancelBooking() {
        isStatusBooked = false
        clearHold()
    }

    fun hasExpiredHold(now: LocalDateTime = LocalDateTime.now()): Boolean =
        holdReservationId != null && holdExpiresAt?.isAfter(now) == false

    private fun isHeld(now: LocalDateTime): Boolean =
        holdReservationId != null && holdExpiresAt?.isAfter(now) == true

    private fun clearExpiredHold(now: LocalDateTime) {
        if (holdReservationId != null && holdExpiresAt?.isAfter(now) != true) {
            clearHold()
        }
    }

    private fun clearHold() {
        holdReservationId = null
        holdExpiresAt = null
    }
}
