package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service

@Service
class ReservationPaymentLock(
    private val entityManager: EntityManager,
    private val reservationRepository: ReservationRepository,
    private val paymentRepository: PaymentRepository,
) {
    fun lockByPaymentId(paymentId: Long): Payment = lock(reservationIdOf(paymentId), paymentId)

    fun lockByRefundId(refundId: Long): Payment {
        val (paymentId, reservationId) = refundIds(refundId)
        return lock(reservationId, paymentId)
    }

    fun lockByPaymentReference(paymentReference: String): Pair<Reservation, Payment> {
        val ids =
            idsWhere(
                "select id, reservation_id from payments where payment_reference = :value",
                paymentReference,
            ) ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentReference")
        return lockedPair(ids)
    }

    fun lockByOrderId(orderId: String): Payment? {
        val ids =
            idsWhere(
                "select id, reservation_id from payments where order_id = :value",
                orderId,
            ) ?: return null
        return lockedPair(ids).second
    }

    fun lock(
        reservationId: Long,
        paymentId: Long,
    ): Payment {
        reservationRepository.findByIdForUpdate(reservationId)
            ?: throw IllegalArgumentException("예약을 찾을 수 없습니다: $reservationId")
        return paymentRepository.findByIdForUpdate(paymentId)
            ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
    }

    private fun lockedPair(ids: Pair<Long, Long>): Pair<Reservation, Payment> {
        val (paymentId, reservationId) = ids
        val payment = lock(reservationId, paymentId)
        return payment.reservation to payment
    }

    private fun refundIds(refundId: Long): Pair<Long, Long> {
        val row =
            entityManager
                .createNativeQuery("select payment_id, reservation_id from payment_refunds where id = :id")
                .setParameter("id", refundId)
                .resultList
                .firstOrNull()
        return pairOf(row) ?: throw IllegalArgumentException("환불 이력을 찾을 수 없습니다: $refundId")
    }

    private fun reservationIdOf(paymentId: Long): Long {
        val found =
            entityManager
                .createNativeQuery("select reservation_id from payments where id = :paymentId")
                .setParameter("paymentId", paymentId)
                .resultList
                .firstOrNull()
        return (found as? Number)?.toLong()
            ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
    }

    private fun idsWhere(
        sql: String,
        value: String,
    ): Pair<Long, Long>? {
        val row =
            entityManager
                .createNativeQuery(sql)
                .setParameter("value", value)
                .resultList
                .firstOrNull()
        return pairOf(row)
    }

    private fun pairOf(row: Any?): Pair<Long, Long>? {
        val values =
            when (row) {
                is Array<*> -> row
                is List<*> -> row.toTypedArray()
                else -> emptyArray()
            }
        val paymentId = values.getOrNull(0) as? Number
        val reservationId = values.getOrNull(1) as? Number
        if (paymentId == null || reservationId == null) {
            return null
        }
        return paymentId.toLong() to reservationId.toLong()
    }
}
