package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.PaymentLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PaymentLogJpaRepository : JpaRepository<PaymentLog, Long> {
    fun findAllByPaymentIdOrderByCreatedAtDesc(paymentId: Long): List<PaymentLog>

    @Query(
        """
        select case when count(l.id) > 0 then true else false end
        from PaymentLog l
        where l.payment.id = :paymentId
          and l.errorMessage = :errorMessage
        """,
    )
    fun existsByPaymentIdAndErrorMessage(
        @Param("paymentId") paymentId: Long,
        @Param("errorMessage") errorMessage: String,
    ): Boolean
}
