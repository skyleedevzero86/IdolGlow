package com.sleekydz86.idolglow.productpackage.reservation.application

import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.SlotWaitlistEntryResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlotWaitlistEntry
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import com.sleekydz86.idolglow.productpackage.reservation.infrastructure.ReservationJpaRepository
import com.sleekydz86.idolglow.productpackage.reservation.infrastructure.ReservationSlotJpaRepository
import com.sleekydz86.idolglow.productpackage.reservation.infrastructure.ReservationSlotWaitlistJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Transactional
@Service
class ReservationSlotWaitlistService(
    private val reservationSlotJpaRepository: ReservationSlotJpaRepository,
    private val reservationSlotWaitlistJpaRepository: ReservationSlotWaitlistJpaRepository,
    private val reservationJpaRepository: ReservationJpaRepository,
    private val notificationCommandService: NotificationCommandService,
) {

    fun register(userId: Long, productId: Long, reservationSlotId: Long): SlotWaitlistEntryResponse {
        val slot = reservationSlotJpaRepository.findByIdForUpdate(reservationSlotId)
            ?: throw IllegalArgumentException("예약 슬롯을 찾을 수 없습니다: $reservationSlotId")
        require(slot.product.id == productId) { "해당 슬롯은 상품 ID $productId 에 속하지 않습니다." }

        val now = LocalDateTime.now()
        require(slot.isUnavailableForWaitlistRegistration(now)) {
            "예약 가능한 슬롯에는 웨이팅을 등록할 수 없습니다."
        }

        val activeStatuses = listOf(ReservationStatus.PENDING, ReservationStatus.BOOKED)
        require(
            !reservationJpaRepository.existsByUserIdAndReservationSlotIdAndStatusIn(
                userId,
                reservationSlotId,
                activeStatuses
            )
        ) {
            "이미 해당 시간에 진행 중인 예약이 있습니다."
        }

        require(!reservationSlotWaitlistJpaRepository.existsByUserIdAndReservationSlotId(userId, reservationSlotId)) {
            "이미 웨이팅에 등록되어 있습니다."
        }

        val saved = reservationSlotWaitlistJpaRepository.save(
            ReservationSlotWaitlistEntry(
                userId = userId,
                reservationSlot = slot,
            )
        )
        return SlotWaitlistEntryResponse.from(saved)
    }

    fun unregister(userId: Long, productId: Long, reservationSlotId: Long) {
        val slot = reservationSlotJpaRepository.findWithProductById(reservationSlotId)
            ?: throw IllegalArgumentException("예약 슬롯을 찾을 수 없습니다: $reservationSlotId")
        require(slot.product.id == productId) { "해당 슬롯은 상품 ID $productId 에 속하지 않습니다." }

        val removed = reservationSlotWaitlistJpaRepository.deleteByUserIdAndReservationSlotId(userId, reservationSlotId)
        require(removed > 0) { "웨이팅 등록을 찾을 수 없습니다." }
    }

    fun findMine(userId: Long): List<SlotWaitlistEntryResponse> =
        reservationSlotWaitlistJpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
            .map { SlotWaitlistEntryResponse.from(it) }

    fun notifyWaitersForReleasedSlot(reservationSlotId: Long) {
        val slot = reservationSlotJpaRepository.findWithProductById(reservationSlotId) ?: return
        val now = LocalDateTime.now()
        if (slot.isUnavailableForWaitlistRegistration(now)) {
            return
        }

        val entries = reservationSlotWaitlistJpaRepository.findAllByReservationSlotIdOrderByCreatedAtAsc(reservationSlotId)
        if (entries.isEmpty()) {
            return
        }

        val product = slot.product
        val dateStr = slot.reservationDate.format(DATE_FMT)
        val timeStr = "${formatTime(slot.startTime)}~${formatTime(slot.endTime)}"
        val message = "\"${product.name}\" $dateStr $timeStr 에 빈자리가 생겼습니다. 지금 예약해 보세요."

        entries.forEach { entry ->
            notificationCommandService.create(
                userId = entry.userId,
                type = NotificationType.RESERVATION_SLOT_AVAILABLE,
                title = "예약 가능 알림",
                message = message,
                link = "/products/${product.id}",
            )
        }
        reservationSlotWaitlistJpaRepository.deleteByReservationSlotId(reservationSlotId)
    }

    fun clearWaitlistForSlot(reservationSlotId: Long) {
        reservationSlotWaitlistJpaRepository.deleteByReservationSlotId(reservationSlotId)
    }

    companion object {
        private val DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE
        private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

        private fun formatTime(t: java.time.LocalTime): String = t.format(TIME_FMT)
    }
}
