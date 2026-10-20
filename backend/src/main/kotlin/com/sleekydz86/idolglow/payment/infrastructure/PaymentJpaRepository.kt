package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import jakarta.persistence.LockModeType
import java.time.LocalDateTime
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PaymentJpaRepository : JpaRepository<Payment, Long> {
    fun findByPaymentReference(paymentReference: String): Payment?

    fun findByReservationId(reservationId: Long): Payment?

    fun findByOrderId(orderId: String): Payment?

    fun findByIdempotencyKey(idempotencyKey: String): Payment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.paymentReference = :paymentReference")
    fun findByPaymentReferenceForUpdate(
        @Param("paymentReference") paymentReference: String,
    ): Payment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.reservation.id = :reservationId")
    fun findByReservationIdForUpdate(
        @Param("reservationId") reservationId: Long,
    ): Payment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.orderId = :orderId")
    fun findByOrderIdForUpdate(
        @Param("orderId") orderId: String,
    ): Payment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.idempotencyKey = :idempotencyKey")
    fun findByIdempotencyKeyForUpdate(
        @Param("idempotencyKey") idempotencyKey: String,
    ): Payment?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    fun findPaymentByIdForUpdate(
        @Param("id") id: Long,
    ): Payment?

    @Query(
        """
        select p from Payment p
        join fetch p.reservation
        where p.status = :status and p.updatedAt < :updatedAt
        order by p.updatedAt asc
        """,
    )
    fun findConfirmingUpdatedBefore(
        @Param("status") status: PaymentStatus,
        @Param("updatedAt") updatedAt: LocalDateTime,
        pageable: Pageable,
    ): List<Payment>
}
