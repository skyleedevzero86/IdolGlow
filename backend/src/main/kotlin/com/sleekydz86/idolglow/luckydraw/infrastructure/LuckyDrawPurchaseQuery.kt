package com.sleekydz86.idolglow.luckydraw.infrastructure

import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.productpackage.product.domain.ProductEventSale
import com.sleekydz86.idolglow.productpackage.product.domain.ProductEventStatus
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class LuckyDrawPurchaseQuery(
    private val entityManager: EntityManager,
) {
    fun countEventPurchases(
        userIds: Collection<Long>,
        startsAt: LocalDateTime,
        endsAt: LocalDateTime,
    ): Map<Long, Int> {
        if (userIds.isEmpty()) return emptyMap()
        return findPaidEventRows(userIds, startsAt, endsAt)
            .filter { row -> row.sale.status(row.approvedAt.toLocalDate()) == ProductEventStatus.ACTIVE }
            .groupingBy { it.userId }
            .eachCount()
    }

    private fun findPaidEventRows(
        userIds: Collection<Long>,
        startsAt: LocalDateTime,
        endsAt: LocalDateTime,
    ): List<PaidEventRow> =
        entityManager
            .createQuery(
                """
                select r.userId, p.approvedAt, prod.eventSale
                from Payment p
                join p.reservation r
                join r.reservationSlot s
                join s.product prod
                where p.status = :status
                  and p.approvedAt >= :startsAt
                  and p.approvedAt < :endsAt
                  and r.userId in :userIds
                  and prod.eventSale.startsOn is not null
                """.trimIndent(),
                Array<Any>::class.java,
            ).setParameter("status", PaymentStatus.SUCCEEDED)
            .setParameter("startsAt", startsAt)
            .setParameter("endsAt", endsAt)
            .setParameter("userIds", userIds)
            .resultList
            .map { row -> PaidEventRow(row[0] as Long, row[1] as LocalDateTime, row[2] as ProductEventSale) }

    private data class PaidEventRow(
        val userId: Long,
        val approvedAt: LocalDateTime,
        val sale: ProductEventSale,
    )
}
