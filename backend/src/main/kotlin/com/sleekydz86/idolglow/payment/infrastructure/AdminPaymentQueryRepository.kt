package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class AdminPaymentQueryRepository(
    private val entityManager: EntityManager,
) {

    fun findPayments(
        status: PaymentStatus?,
        visitDate: LocalDate?,
        productId: Long?,
        size: Int,
    ): List<Payment> {
        val conditions = mutableListOf<String>()
        if (status != null) {
            conditions += "p.status = :status"
        }
        if (visitDate != null) {
            conditions += "r.visitDate = :visitDate"
        }
        if (productId != null) {
            conditions += "prod.id = :productId"
        }

        val whereClause = if (conditions.isEmpty()) "" else "where ${conditions.joinToString(" and ")}"

        val query = entityManager.createQuery(
            """
            select p from Payment p
            join fetch p.reservation r
            join fetch r.reservationSlot rs
            join fetch rs.product prod
            $whereClause
            order by p.createdAt desc
            """.trimIndent(),
            Payment::class.java,
        ).setMaxResults(size)

        if (status != null) {
            query.setParameter("status", status)
        }
        if (visitDate != null) {
            query.setParameter("visitDate", visitDate)
        }
        if (productId != null) {
            query.setParameter("productId", productId)
        }

        return query.resultList
    }

    fun findPaymentById(paymentId: Long): Payment? =
        entityManager.createQuery(
            """
            select p from Payment p
            join fetch p.reservation r
            join fetch r.reservationSlot rs
            join fetch rs.product prod
            where p.id = :paymentId
            """.trimIndent(),
            Payment::class.java,
        )
            .setParameter("paymentId", paymentId)
            .resultList
            .firstOrNull()

    fun findPaymentsByUser(
        userId: Long,
        size: Int,
    ): List<Payment> =
        entityManager.createQuery(
            """
            select p from Payment p
            join fetch p.reservation r
            join fetch r.reservationSlot rs
            join fetch rs.product prod
            where r.userId = :userId
            order by p.createdAt desc
            """.trimIndent(),
            Payment::class.java,
        )
            .setParameter("userId", userId)
            .setMaxResults(size)
            .resultList

    fun findPaymentByIdAndUserId(
        paymentId: Long,
        userId: Long,
    ): Payment? =
        entityManager.createQuery(
            """
            select p from Payment p
            join fetch p.reservation r
            join fetch r.reservationSlot rs
            join fetch rs.product prod
            where p.id = :paymentId
              and r.userId = :userId
            """.trimIndent(),
            Payment::class.java,
        )
            .setParameter("paymentId", paymentId)
            .setParameter("userId", userId)
            .resultList
            .firstOrNull()
}
