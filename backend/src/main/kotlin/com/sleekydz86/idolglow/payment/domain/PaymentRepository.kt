package com.sleekydz86.idolglow.payment.domain

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findByPaymentReference(paymentReference: String): Payment?
    fun findByPaymentReferenceForUpdate(paymentReference: String): Payment?
    fun findByReservationId(reservationId: Long): Payment?
    fun findByReservationIdForUpdate(reservationId: Long): Payment?
    fun findByOrderIdForUpdate(orderId: String): Payment?
    fun findByIdempotencyKeyForUpdate(idempotencyKey: String): Payment?
    fun findByIdForUpdate(paymentId: Long): Payment?
}
