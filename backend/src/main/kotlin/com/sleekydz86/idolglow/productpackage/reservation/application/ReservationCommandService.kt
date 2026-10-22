package com.sleekydz86.idolglow.productpackage.reservation.application

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.notification.application.NotificationCommandService
import com.sleekydz86.idolglow.notification.domain.NotificationRepository
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.payment.application.GlowPassBenefitPolicy
import com.sleekydz86.idolglow.payment.application.MembershipQuery
import com.sleekydz86.idolglow.payment.application.PaymentRefundService
import com.sleekydz86.idolglow.payment.application.PaymentViewPolicyService
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
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
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Transactional
@Service
class ReservationCommandService(
    private val reservationRepository: ReservationRepository,
    private val reservationSlotRepository: ReservationSlotRepository,
    private val productCommandRepository: ProductCommandRepository,
    private val paymentRepository: PaymentRepository,
    private val entityManager: EntityManager,
    private val scheduleRepository: ScheduleRepository,
    private val notificationCommandService: NotificationCommandService,
    private val notificationRepository: NotificationRepository,
    private val reservationSlotWaitlistService: ReservationSlotWaitlistService,
    private val paymentRefundService: PaymentRefundService,
    private val tossPaymentProperties: TossPaymentProperties,
    private val membershipQuery: MembershipQuery,
    private val glowPassBenefitPolicy: GlowPassBenefitPolicy,
    private val paymentViewPolicyService: PaymentViewPolicyService,
    @Value("\${reservation.pending-timeout-seconds:900}")
    private val pendingTimeoutSeconds: Long,
) {
    fun createReservation(command: CreateReservationCommand): ReservationCreatedResponse {
        val now = LocalDateTime.now()
        val reservationSlot =
            findReservationTimeSlotByReservationSlotIdWithPessimisticLock(command.reservationSlotId)
        reservationSlot.validateAvailability(command.productId, now)
        glowPassBenefitPolicy.assertEarlyBookingAllowed(command.userId, reservationSlot, now)

        val product = findProductByProductId(command.productId)
        val pricing = resolveMembershipPricing(product, command.userId, command.totalPrice, now)
        val expiresAt = now.plusSeconds(pendingTimeoutSeconds)

        val reservation =
            Reservation(
                reservationSlot = reservationSlot,
                userId = command.userId,
                visitDate = reservationSlot.reservationDate,
                visitStartTime = reservationSlot.startTime,
                visitEndTime = reservationSlot.endTime,
                totalPrice = pricing.expectedTotal,
                membershipDiscount = pricing.discount,
            ).request(expiresAt)

        val savedReservation = reservationRepository.save(reservation)
        reservationSlot.hold(savedReservation.id, expiresAt, now)
        val paymentRef =
            paymentReferenceOf(
                useTossProvider = tossPaymentProperties.useTossProvider,
                reservationId = savedReservation.id,
                now = now,
            )
        val payment =
            paymentRepository.save(
                if (tossPaymentProperties.useTossProvider) {
                    Payment.createPendingForToss(savedReservation, paymentRef)
                } else {
                    Payment.createMock(savedReservation, paymentRef)
                },
            )

        return ReservationCreatedResponse.from(savedReservation, payment)
    }

    fun cancelMyPagePayment(
        userId: Long,
        paymentId: Long,
        cancelReasonText: String? = null,
    ): ReservationCancelOutcome {
        val reservationId = reservationIdOfPayment(paymentId)
        findReservationByReservationIdForUpdate(reservationId)
        val payment =
            paymentRepository.findByIdForUpdate(paymentId)
                ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
        require(payment.reservation.userId == userId) { "본인 결제만 취소할 수 있습니다." }
        rejectIfPaymentConfirming(payment)
        require(paymentViewPolicyService.canUserCancel(payment)) {
            "사용자 결제 취소 가능 기간(15일)이 지났거나 취소할 수 없는 상태입니다."
        }
        return cancelReservationByUser(reservationId, userId, cancelReasonText)
    }

    fun cancelReservationByUser(
        reservationId: Long,
        userId: Long,
        cancelReasonText: String? = null,
    ): ReservationCancelOutcome {
        val reservation = findReservationByReservationIdForUpdate(reservationId)
        reservation.validateOwner(userId)
        val payment = paymentRepository.findByReservationIdForUpdate(reservationId)
        rejectIfPaymentConfirming(payment)
        val cancelReason = cancelReasonText?.trim()?.takeIf { it.isNotEmpty() } ?: "사용자가 예약을 취소했습니다."
        return finishCancel(
            reservation = reservation,
            payment = payment,
            cancelReason = cancelReason,
            requestedBy = RefundRequestedBy.USER,
            userId = userId,
            reservationCancelReason = ReservationCancelReason.USER_REQUESTED,
            notificationTitle = "예약 취소",
            notificationMessage = "예약 #${reservation.id} 이(가) 취소되었습니다.",
        )
    }

    fun cancelAdminPayment(
        paymentId: Long,
        cancelReasonText: String? = null,
    ): ReservationCancelOutcome {
        val reservationId = reservationIdOfPayment(paymentId)
        findReservationByReservationIdForUpdate(reservationId)
        val payment =
            paymentRepository.findByIdForUpdate(paymentId)
                ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
        rejectIfPaymentConfirming(payment)
        require(paymentViewPolicyService.canAdminCancel(payment)) { "관리자 취소가 가능한 결제 상태가 아닙니다." }
        return cancelReservationByAdmin(reservationId, cancelReasonText)
    }

    fun cancelReservationByAdmin(
        reservationId: Long,
        cancelReasonText: String? = null,
    ): ReservationCancelOutcome {
        val reservation = findReservationByReservationIdForUpdate(reservationId)
        val payment = paymentRepository.findByReservationIdForUpdate(reservationId)
        rejectIfPaymentConfirming(payment)
        val cancelReason = cancelReasonText?.trim()?.takeIf { it.isNotEmpty() } ?: "운영자가 예약을 취소했습니다."
        return finishCancel(
            reservation = reservation,
            payment = payment,
            cancelReason = cancelReason,
            requestedBy = RefundRequestedBy.ADMIN,
            userId = null,
            reservationCancelReason = ReservationCancelReason.ADMIN_CANCELED,
            notificationTitle = "운영자 예약 취소",
            notificationMessage = "예약 #${reservation.id} 이(가) 운영자에 의해 취소되었습니다.",
        )
    }

    private fun finishCancel(
        reservation: Reservation,
        payment: Payment?,
        cancelReason: String,
        requestedBy: RefundRequestedBy,
        userId: Long?,
        reservationCancelReason: ReservationCancelReason,
        notificationTitle: String,
        notificationMessage: String,
    ): ReservationCancelOutcome {
        val refund =
            if (reservation.status == ReservationStatus.BOOKED && payment?.status == PaymentStatus.SUCCEEDED) {
                if (requestedBy == RefundRequestedBy.USER) {
                    glowPassBenefitPolicy.assertFreeCancelAllowed(userId!!, reservation)
                }
                paymentRefundService.refundBeforeReservationCancel(payment, reservation, cancelReason, requestedBy)
            } else {
                null
            }
        if (payment?.status == PaymentStatus.PENDING) {
            payment.markCanceled(cancelReason)
        }
        cancelReservationInternal(
            reservation = reservation,
            reason = reservationCancelReason,
            notificationType = NotificationType.RESERVATION_CANCELED,
            notificationTitle = notificationTitle,
            notificationMessage = notificationMessage,
        )
        return ReservationCancelOutcome(reservation.id, refund)
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
            link = "/reservations/${reservation.id}",
        )
        return reservation
    }

    fun notifyExpiringSoonReservations(warningSeconds: Long = 300) {
        val now = LocalDateTime.now()
        val warningThreshold = now.plusSeconds(warningSeconds)
        val soonExpiredIds = reservationRepository.findExpiringSoonPendingIds(warningThreshold, now)
        soonExpiredIds.forEach { reservationId ->
            val reservation = reservationRepository.findByIdForUpdate(reservationId) ?: return@forEach
            if (reservation.status != ReservationStatus.PENDING) return@forEach
            val link = "/reservations/${reservation.id}"
            if (notificationRepository.existsByUserIdAndTypeAndLink(
                    reservation.userId,
                    NotificationType.RESERVATION_EXPIRING_SOON,
                    link,
                )
            ) {
                return@forEach
            }
            notificationCommandService.create(
                userId = reservation.userId,
                type = NotificationType.RESERVATION_EXPIRING_SOON,
                title = "예약 만료 임박",
                message = "예약 #${reservation.id} 결제 시간이 곧 만료됩니다. 지금 결제를 완료해 주세요.",
                link = link,
            )
        }
    }

    fun expirePendingReservation(
        reservationId: Long,
        now: LocalDateTime = LocalDateTime.now(),
    ): Reservation? {
        val reservation = findReservationByReservationIdForUpdate(reservationId)
        if (!reservation.isExpired(now)) {
            return reservation
        }
        expireClosedReservation(reservation, now)
        return reservation
    }

    private fun expireClosedReservation(
        reservation: Reservation,
        now: LocalDateTime,
    ) {
        val payment = paymentRepository.findByReservationIdForUpdate(reservation.id)
        if (payment?.status == PaymentStatus.CONFIRMING) {
            return
        }
        if (payment?.status == PaymentStatus.PENDING) {
            payment.markExpired(now)
        }
        cancelReservationInternal(
            reservation = reservation,
            reason = ReservationCancelReason.PAYMENT_EXPIRED,
            notificationType = NotificationType.PAYMENT_EXPIRED,
            notificationTitle = "예약 만료",
            notificationMessage = "예약 #${reservation.id} 결제가 완료되기 전에 예약이 만료되었습니다.",
        )
    }

    private fun reservationIdOfPayment(paymentId: Long): Long {
        val found =
            entityManager
                .createNativeQuery("select reservation_id from payments where id = :paymentId")
                .setParameter("paymentId", paymentId)
                .resultList
                .firstOrNull()
        return (found as? Number)?.toLong()
            ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
    }

    private fun rejectIfPaymentConfirming(payment: Payment?) {
        require(payment?.status != PaymentStatus.CONFIRMING) {
            "결제 처리 중입니다. 잠시 후 다시 시도해 주세요."
        }
        require(payment?.status != PaymentStatus.REFUNDING) {
            "환불 처리 중입니다. 잠시 후 다시 시도해 주세요."
        }
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
            link = "/reservations/${reservation.id}",
        )
        reservationSlotWaitlistService.notifyWaitersForReleasedSlot(reservation.reservationSlot.id)
    }

    private fun ensureScheduleExists(reservation: Reservation) {
        val startAt = LocalDateTime.of(reservation.visitDate, reservation.visitStartTime)
        val endAt = LocalDateTime.of(reservation.visitDate, reservation.visitEndTime)
        val existing =
            scheduleRepository.findByReservationContext(
                userId = reservation.userId,
                productId = reservation.reservationSlot.product.id,
                startAt = startAt,
                endAt = endAt,
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
                endAt = endAt,
            ),
        )
    }

    private fun removeScheduleIfExists(reservation: Reservation) {
        val schedule =
            scheduleRepository.findByReservationContext(
                userId = reservation.userId,
                productId = reservation.reservationSlot.product.id,
                startAt = LocalDateTime.of(reservation.visitDate, reservation.visitStartTime),
                endAt = LocalDateTime.of(reservation.visitDate, reservation.visitEndTime),
            )
        if (schedule != null) {
            scheduleRepository.delete(schedule)
        }
    }

    private fun resolveMembershipPricing(
        product: Product,
        userId: Long,
        requestedTotalPrice: BigDecimal,
        at: LocalDateTime,
    ): MembershipPricing {
        val fullPrice = product.totalPrice
        val member = membershipQuery.isActive(userId, at)
        val expectedTotal =
            if (member) {
                fullPrice
                    .multiply(MEMBER_PRICE_RATE)
                    .setScale(0, RoundingMode.HALF_UP)
                    .setScale(2)
            } else {
                fullPrice
            }
        if (expectedTotal.compareTo(requestedTotalPrice) != 0) {
            throw IllegalArgumentException(
                "상품 ${product.id}의 총금액이 일치하지 않습니다. " +
                    "requested=$requestedTotalPrice expected=$expectedTotal member=$member",
            )
        }
        val discount = fullPrice.subtract(expectedTotal).max(BigDecimal.ZERO).setScale(2)
        return MembershipPricing(expectedTotal = expectedTotal, discount = discount)
    }

    private data class MembershipPricing(
        val expectedTotal: BigDecimal,
        val discount: BigDecimal,
    )

    companion object {
        private val PAYMENT_REFERENCE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        private val MEMBER_PRICE_RATE = BigDecimal("0.9")

        internal fun paymentReferenceOf(
            useTossProvider: Boolean,
            reservationId: Long,
            now: LocalDateTime,
        ): String {
            val prefix = if (useTossProvider) "pay_toss_" else "pay_mock_"
            return "$prefix${now.format(PAYMENT_REFERENCE_TIME_FORMATTER)}_$reservationId"
        }
    }
}
