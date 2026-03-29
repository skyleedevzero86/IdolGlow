package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import org.springframework.stereotype.Repository

@Repository
class PaymentRepositoryImpl(
    private val paymentJpaRepository: PaymentJpaRepository
) : PaymentRepository {

    override fun save(payment: Payment): Payment =
        paymentJpaRepository.save(payment)

    override fun findByPaymentReference(paymentReference: String): Payment? =
        paymentJpaRepository.findByPaymentReference(paymentReference)

    override fun findByPaymentReferenceForUpdate(paymentReference: String): Payment? =
        paymentJpaRepository.findByPaymentReferenceForUpdate(paymentReference)

    override fun findByReservationId(reservationId: Long): Payment? =
        paymentJpaRepository.findByReservationId(reservationId)

    override fun findByReservationIdForUpdate(reservationId: Long): Payment? =
        paymentJpaRepository.findByReservationIdForUpdate(reservationId)
}
