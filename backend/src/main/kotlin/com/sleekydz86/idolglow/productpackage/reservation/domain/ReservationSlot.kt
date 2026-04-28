package com.sleekydz86.idolglow.productpackage.reservation.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
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
    val endTime: LocalTime,

    @Column(name = "admin_note", columnDefinition = "TEXT")
    var adminNote: String? = null,
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
        require(startTime.minute == 0 && endTime.minute == 0) { "예약 슬롯은 정각 단위로만 생성할 수 있습니다." }
        require(startTime.hour in 9..15) { "예약 시작 시간은 09:00부터 15:00 사이여야 합니다." }
        require(endTime == startTime.plusHours(1)) { "예약 슬롯은 정확히 1시간이어야 합니다." }
        require(!duration.isZero && !duration.isNegative) { "예약 종료 시간은 시작 시간보다 늦어야 합니다." }
    }

    fun validateAvailability(productId: Long, now: LocalDateTime = LocalDateTime.now()) {
        clearExpiredHold(now)
        require(!isStatusBooked) { "이미 예약된 슬롯입니다." }
        require(this.product.id == productId) { "해당 슬롯은 상품 ID $productId 에 속하지 않습니다." }
        require(!isHeld(now)) { "다른 예약이 이미 선점한 슬롯입니다." }
    }

    fun hold(reservationId: Long, expiresAt: LocalDateTime, now: LocalDateTime = LocalDateTime.now()) {
        clearExpiredHold(now)
        if (isStatusBooked) {
            throw IllegalStateException("이미 예약된 슬롯입니다.")
        }
        if (isHeld(now) && holdReservationId != reservationId) {
            throw IllegalStateException("이미 선점 중인 슬롯입니다.")
        }
        holdReservationId = reservationId
        holdExpiresAt = expiresAt
    }

    fun confirmBooking(reservationId: Long, now: LocalDateTime = LocalDateTime.now()) {
        clearExpiredHold(now)
        require(!isStatusBooked) { "이미 예약된 슬롯입니다." }
        require(holdReservationId == reservationId) { "현재 예약이 선점한 슬롯이 아닙니다." }
        require(holdExpiresAt?.isAfter(now) == true) { "슬롯 선점 시간이 이미 만료되었습니다." }
        isStatusBooked = true
        clearHold()
    }

    fun releaseHold(reservationId: Long) {
        if (holdReservationId == null) {
            return
        }
        require(holdReservationId == reservationId) { "다른 예약이 선점한 슬롯입니다." }
        clearHold()
    }

    fun cancelBooking() {
        isStatusBooked = false
        clearHold()
    }

    fun updateAdminNote(note: String?) {
        adminNote = note?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun hasExpiredHold(now: LocalDateTime = LocalDateTime.now()): Boolean =
        holdReservationId != null && holdExpiresAt?.isAfter(now) == false

    fun isUnavailableForWaitlistRegistration(now: LocalDateTime = LocalDateTime.now()): Boolean {
        clearExpiredHold(now)
        if (isStatusBooked) {
            return true
        }
        return holdReservationId != null && holdExpiresAt?.isAfter(now) == true
    }

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
