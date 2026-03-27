package com.sleekydz86.idolglow.productpackage.reservation.application

import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductCommandRepository
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.CreateReservationCommand
import com.sleekydz86.idolglow.productpackage.reservation.application.dto.ReservationCreatedResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationCancelReason
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlotRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import com.sleekydz86.idolglow.schedule.domain.Schedule
import com.sleekydz86.idolglow.schedule.domain.ScheduleRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Transactional
@Service
class ReservationCommandService(
    private val reservationRepository: ReservationRepository,
    private val reservationSlotRepository: ReservationSlotRepository,
    private val productCommandRepository: ProductCommandRepository,
    private val paymentRepository: PaymentRepository,
    private val scheduleRepository: ScheduleRepository,
    private val notificationCommandService: NotificationCommandService,
    @Value("\${reservation.pending-timeout-seconds:900}")
    private val pendingTimeoutSeconds: Long,
) {

    fun createReservation(command: CreateReservationCommand): ReservationCreatedResponse {
        val now = LocalDateTime.now()
        val reservationSlot =
            findReservationTimeSlotByReservationSlotIdWithPessimisticLock(command.reservationSlotId)
        reservationSlot.validateAvailability(command.productId, now)

        val product = findProductByProductId(command.productId)
        product.validateTotalPrice(command.totalPrice)
        val expiresAt = now.plusSeconds(pendingTimeoutSeconds)

        val reservation = Reservation(
            reservationSlot = reservationSlot,
            userId = command.userId,
            visitDate = reservationSlot.reservationDate,
            visitStartTime = reservationSlot.startTime,
            visitEndTime = reservationSlot.endTime,
            totalPrice = command.totalPrice
        ).request(expiresAt)

        val savedReservation = reservationRepository.save(reservation)
        reservationSlot.hold(savedReservation.id, expiresAt, now)
        val payment = paymentRepository.save(
            Payment.createMock(savedReservation, createPaymentReference(savedReservation.id, now))
        )

        return ReservationCreatedResponse.from(savedReservation, payment)
    }

    fun cancelReservationByUser(reservationId: Long, userId: Long): Reservation {
        val reservation = findReservationByReservationIdForUpdate(reservationId)
        reservation.validateOwner(userId)
        val payment = paymentRepository.findByReservationIdForUpdate(reservationId)
        if (payment?.status == PaymentStatus.PENDING) {
            payment.markCanceled("reservation canceled by user")
        }
        cancelReservationInternal(
            reservation = reservation,
            reason = ReservationCancelReason.USER_REQUESTED,
            notificationType = NotificationType.RESERVATION_CANCELED,
            notificationTitle = "Reservation canceled",
            notificationMessage = "Your reservation #${reservation.id} has been canceled."
        )
        return reservation
    }

    fun cancelReservationByAdmin(reservationId: Long): Reservation {
        val reservation = findReservationByReservationIdForUpdate(reservationId)
        val payment = paymentRepository.findByReservationIdForUpdate(reservationId)
        if (payment?.status == PaymentStatus.PENDING) {
            payment.markCanceled("reservation canceled by admin")
        }
        cancelReservationInternal(
            reservation = reservation,
            reason = ReservationCancelReason.ADMIN_CANCELED,
            notificationType = NotificationType.RESERVATION_CANCELED,
            notificationTitle = "Reservation canceled by admin",
            notificationMessage = "Your reservation #${reservation.id} has been canceled by the operator."
        )
        return reservation
    }

    fun confirmReservation(reservationId: Long): Reservation {
        val reservation = findReservationByReservationIdForUpdate(reservationId)
        val now = LocalDateTime.now()
        if (reservation.status == ReservationStatus.BOOKED) {
            ensureScheduleExists(reservation)
            return reservation
        }
        require(reservation.status == ReservationStatus.PENDING) { "Only pending reservations can be confirmed." }
        require(!reservation.isExpired(now)) { "Reservation already expired." }
        reservation.confirm(now)
        ensureScheduleExists(reservation)
        notificationCommandService.create(
            userId = reservation.userId,
            type = NotificationType.RESERVATION_CONFIRMED,
            title = "Reservation confirmed",
            message = "Your reservation #${reservation.id} has been confirmed.",
            link = "/reservations/${reservation.id}"
        )
        return reservation
    }

    fun expirePendingReservations(batchSize: Int = 100) {
        while (true) {
            val now = LocalDateTime.now()
            val expiredIds = reservationRepository.findExpiredPendingIds(batchSize, now)
            if (expiredIds.isEmpty()) {
                return
            }
            expiredIds.forEach { reservationId ->
                expirePendingReservation(reservationId, now)
            }
        }
    }

    fun expirePendingReservation(reservationId: Long, now: LocalDateTime = LocalDateTime.now()): Reservation? {
        val reservation = findReservationByReservationIdForUpdate(reservationId)
        if (!reservation.isExpired(now)) {
            return reservation
        }
        val payment = paymentRepository.findByReservationIdForUpdate(reservationId)
        if (payment?.status == PaymentStatus.PENDING) {
            payment.markExpired(now)
        }
        cancelReservationInternal(
            reservation = reservation,
            reason = ReservationCancelReason.PAYMENT_EXPIRED,
            notificationType = NotificationType.PAYMENT_EXPIRED,
            notificationTitle = "Reservation expired",
            notificationMessage = "Your reservation #${reservation.id} expired before payment was completed."
        )
        return reservation
    }

    private fun findReservationTimeSlotByReservationSlotIdWithPessimisticLock(reservationSlotId: Long): ReservationSlot =
        reservationSlotRepository.findByIdForUpdate(reservationSlotId)
            ?: throw IllegalArgumentException("Reservation slot not found: $reservationSlotId")

    private fun findProductByProductId(productId: Long): Product =
        productCommandRepository.findById(productId)
            ?: throw IllegalArgumentException("Product not found: $productId")

    private fun findReservationByReservationIdForUpdate(reservationId: Long): Reservation =
        reservationRepository.findByIdForUpdate(reservationId)
            ?: throw IllegalArgumentException("Reservation not found: $reservationId")

    private fun cancelReservationInternal(
        reservation: Reservation,
        reason: ReservationCancelReason,
        notificationType: NotificationType,
        notificationTitle: String,
        notificationMessage: String,
    ) {
        if (reservation.status == ReservationStatus.CANCELED) {
            return
        }
        reservation.cancel(reason)
        removeScheduleIfExists(reservation)
        notificationCommandService.create(
            userId = reservation.userId,
            type = notificationType,
            title = notificationTitle,
            message = notificationMessage,
            link = "/reservations/${reservation.id}"
        )
    }

    private fun ensureScheduleExists(reservation: Reservation) {
        val startAt = LocalDateTime.of(reservation.visitDate, reservation.visitStartTime)
        val endAt = LocalDateTime.of(reservation.visitDate, reservation.visitEndTime)
        val existing = scheduleRepository.findByReservationContext(
            userId = reservation.userId,
            productId = reservation.reservationSlot.product.id,
            startAt = startAt,
            endAt = endAt
        )
        if (existing != null) {
            return
        }
        scheduleRepository.save(
            Schedule.of(
                userId = reservation.userId,
                productId = reservation.reservationSlot.product.id,
                title = reservation.reservationSlot.product.name,
                startAt = startAt,
                endAt = endAt
            )
        )
    }

    private fun removeScheduleIfExists(reservation: Reservation) {
        val schedule = scheduleRepository.findByReservationContext(
            userId = reservation.userId,
            productId = reservation.reservationSlot.product.id,
            startAt = LocalDateTime.of(reservation.visitDate, reservation.visitStartTime),
            endAt = LocalDateTime.of(reservation.visitDate, reservation.visitEndTime)
        )
        if (schedule != null) {
            scheduleRepository.delete(schedule)
        }
    }

    private fun createPaymentReference(reservationId: Long, now: LocalDateTime): String =
        "pay_mock_${now.format(PAYMENT_REFERENCE_TIME_FORMATTER)}_$reservationId"

    companion object {
        private val PAYMENT_REFERENCE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    }
}
