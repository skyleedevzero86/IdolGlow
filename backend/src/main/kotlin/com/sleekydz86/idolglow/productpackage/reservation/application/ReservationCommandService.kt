package com.sleekydz86.idolglow.productpackage.reservation.application

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationRepository
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.payment.application.PaymentRefundService
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.RefundRequestedBy
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
    private val notificationRepository: NotificationRepository,
    private val reservationSlotWaitlistService: ReservationSlotWaitlistService,
    private val paymentRefundService: PaymentRefundService,
    private val tossPaymentProperties: TossPaymentProperties,
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
        val paymentRef = createPaymentReference(savedReservation.id, now)
        val payment = paymentRepository.save(
            if (tossPaymentProperties.useTossProvider) {
                Payment.createPendingForToss(savedReservation, paymentRef)
            } else {
                Payment.createMock(savedReservation, paymentRef)
            }
        )

        return ReservationCreatedResponse.from(savedReservation, payment)
    }

    fun cancelReservationByUser(reservationId: Long, userId: Long): Reservation {
        val reservation = findReservationByReservationIdForUpdate(reservationId)
        reservation.validateOwner(userId)
        val payment = paymentRepository.findByReservationIdForUpdate(reservationId)
        if (reservation.status == ReservationStatus.BOOKED && payment?.status == PaymentStatus.SUCCEEDED) {
            paymentRefundService.refundBeforeReservationCancel(
                payment = payment,
                reservation = reservation,
                cancelReason = "사용자가 예약을 취소했습니다.",
                requestedBy = RefundRequestedBy.USER,
            )
        }
        if (payment?.status == PaymentStatus.PENDING) {
            payment.markCanceled("사용자가 예약을 취소했습니다.")
        }
        cancelReservationInternal(
            reservation = reservation,
            reason = ReservationCancelReason.USER_REQUESTED,
            notificationType = NotificationType.RESERVATION_CANCELED,
            notificationTitle = "예약 취소",
            notificationMessage = "예약 #${reservation.id} 이(가) 취소되었습니다."
        )
        return reservation
    }

    fun cancelReservationByAdmin(reservationId: Long): Reservation {
        val reservation = findReservationByReservationIdForUpdate(reservationId)
        val payment = paymentRepository.findByReservationIdForUpdate(reservationId)
        if (reservation.status == ReservationStatus.BOOKED && payment?.status == PaymentStatus.SUCCEEDED) {
            paymentRefundService.refundBeforeReservationCancel(
                payment = payment,
                reservation = reservation,
                cancelReason = "운영자가 예약을 취소했습니다.",
                requestedBy = RefundRequestedBy.ADMIN,
            )
        }
        if (payment?.status == PaymentStatus.PENDING) {
            payment.markCanceled("운영자가 예약을 취소했습니다.")
        }
        cancelReservationInternal(
            reservation = reservation,
            reason = ReservationCancelReason.ADMIN_CANCELED,
            notificationType = NotificationType.RESERVATION_CANCELED,
            notificationTitle = "운영자 예약 취소",
            notificationMessage = "예약 #${reservation.id} 이(가) 운영자에 의해 취소되었습니다."
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
        require(reservation.status == ReservationStatus.PENDING) { "대기 중인 예약만 확정할 수 있습니다." }
        require(!reservation.isExpired(now)) { "이미 만료된 예약입니다." }
        reservation.confirm(now)
        ensureScheduleExists(reservation)
        notificationCommandService.create(
            userId = reservation.userId,
            type = NotificationType.RESERVATION_CONFIRMED,
            title = "예약 확정",
            message = "예약 #${reservation.id} 이(가) 확정되었습니다.",
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

    fun notifyExpiringSoonReservations(warningSeconds: Long = 300) {
        val now = LocalDateTime.now()
        val warningThreshold = now.plusSeconds(warningSeconds)
        val soonExpiredIds = reservationRepository.findExpiringSoonPendingIds(warningThreshold, now)
        soonExpiredIds.forEach { reservationId ->
            val reservation = reservationRepository.findByIdForUpdate(reservationId) ?: return@forEach
            if (reservation.status != ReservationStatus.PENDING) return@forEach
            val link = "/reservations/${reservation.id}"
            if (notificationRepository.existsByUserIdAndTypeAndLink(reservation.userId, NotificationType.RESERVATION_EXPIRING_SOON, link)) return@forEach
            notificationCommandService.create(
                userId = reservation.userId,
                type = NotificationType.RESERVATION_EXPIRING_SOON,
                title = "예약 만료 임박",
                message = "예약 #${reservation.id} 결제 시간이 곧 만료됩니다. 지금 결제를 완료해 주세요.",
                link = link,
            )
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
            notificationTitle = "예약 만료",
            notificationMessage = "예약 #${reservation.id} 결제가 완료되기 전에 예약이 만료되었습니다."
        )
        return reservation
    }

    private fun findReservationTimeSlotByReservationSlotIdWithPessimisticLock(reservationSlotId: Long): ReservationSlot =
        reservationSlotRepository.findByIdForUpdate(reservationSlotId)
            ?: throw IllegalArgumentException("예약 슬롯을 찾을 수 없습니다: $reservationSlotId")

    private fun findProductByProductId(productId: Long): Product =
        productCommandRepository.findById(productId)
            ?: throw IllegalArgumentException("상품을 찾을 수 없습니다: $productId")

    private fun findReservationByReservationIdForUpdate(reservationId: Long): Reservation =
        reservationRepository.findByIdForUpdate(reservationId)
            ?: throw IllegalArgumentException("예약을 찾을 수 없습니다: $reservationId")

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
        reservationSlotWaitlistService.notifyWaitersForReleasedSlot(reservation.reservationSlot.id)
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
