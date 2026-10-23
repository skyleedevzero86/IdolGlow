package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.PaymentRefund
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface PaymentRefundJpaRepository : JpaRepository<PaymentRefund, Long> {
    fun findAllByPaymentIdOrderByCreatedAtDesc(paymentId: Long): List<PaymentRefund>

    @Query(
        """
        select r.payment.id from PaymentRefund r
        where r.status = com.sleekydz86.idolglow.payment.domain.PaymentRefundStatus.PENDING
          and r.payment.status = com.sleekydz86.idolglow.payment.domain.PaymentStatus.REFUNDING
          and r.payment.provider = com.sleekydz86.idolglow.payment.domain.PaymentProvider.TOSS
          and r.firstRequestedAt < :requestedBefore
          and (
            r.firstRequestedAt >= :stopRetryBefore
            or not exists (
              select l.id from PaymentLog l
              where l.payment.id = r.payment.id
                and l.errorMessage = :stopMessage
                and l.createdAt >= :stopLoggedSince
            )
          )
        order by r.firstRequestedAt asc
        """,
    )
    fun findIdsToReconcile(
        @Param("requestedBefore") requestedBefore: LocalDateTime,
        @Param("stopRetryBefore") stopRetryBefore: LocalDateTime,
        @Param("stopMessage") stopMessage: String,
        @Param("stopLoggedSince") stopLoggedSince: LocalDateTime,
        pageable: Pageable,
    ): List<Long>
}
