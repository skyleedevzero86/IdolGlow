package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType

interface PaymentJpaRepository : JpaRepository<Payment, Long> {

    fun findByPaymentReference(paymentReference: String): Payment?

    fun findByReservationId(reservationId: Long): Payment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.reservation where p.paymentReference = :paymentReference")
    fun findByPaymentReferenceForUpdate(@Param("paymentReference") paymentReference: String): Payment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.reservation where p.reservation.id = :reservationId")
    fun findByReservationIdForUpdate(@Param("reservationId") reservationId: Long): Payment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.reservation where p.orderId = :orderId")
    fun findByOrderIdForUpdate(@Param("orderId") orderId: String): Payment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.reservation where p.idempotencyKey = :idempotencyKey")
    fun findByIdempotencyKeyForUpdate(@Param("idempotencyKey") idempotencyKey: String): Payment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.reservation where p.id = :id")
    fun findPaymentByIdForUpdate(@Param("id") id: Long): Payment?
}
