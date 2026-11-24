package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import java.time.LocalDateTime
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class PaymentRepositoryImpl(
    private val paymentJpaRepository: PaymentJpaRepository,
) : PaymentRepository {
    override fun save(payment: Payment): Payment = paymentJpaRepository.save(payment)

    override fun findByPaymentReference(paymentReference: String): Payment? = paymentJpaRepository.findByPaymentReference(paymentReference)

    override fun findByPaymentReferenceForUpdate(paymentReference: String): Payment? =
        paymentJpaRepository.findByPaymentReferenceForUpdate(paymentReference)

    override fun findByReservationId(reservationId: Long): Payment? = paymentJpaRepository.findByReservationId(reservationId)

    override fun findByReservationIdForUpdate(reservationId: Long): Payment? =
        paymentJpaRepository.findByReservationIdForUpdate(reservationId)

    override fun findByOrderId(orderId: String): Payment? = paymentJpaRepository.findByOrderId(orderId)

    override fun findByOrderIdForUpdate(orderId: String): Payment? = paymentJpaRepository.findByOrderIdForUpdate(orderId)

    override fun findByIdempotencyKey(idempotencyKey: String): Payment? =
        paymentJpaRepository.findByIdempotencyKey(idempotencyKey)

    override fun findByIdempotencyKeyForUpdate(idempotencyKey: String): Payment? =
        paymentJpaRepository.findByIdempotencyKeyForUpdate(idempotencyKey)

    override fun findByIdForUpdate(paymentId: Long): Payment? = paymentJpaRepository.findPaymentByIdForUpdate(paymentId)

    override fun findConfirmingUpdatedBefore(
        before: LocalDateTime,
        limit: Int,
    ): List<Payment> =
        paymentJpaRepository
            .findConfirmingUpdatedBefore(PaymentStatus.CONFIRMING, before, PageRequest.of(0, limit))
}
