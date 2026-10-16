package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.hibernate.Session
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
class TossBillingDueClaimRepository(
    @PersistenceContext
    private val entityManager: EntityManager,
) {
    @Transactional
    fun claimDueSubscriptionIds(
        now: LocalDateTime,
        limit: Int = 100,
    ): List<Long> {
        val sql =
            if (supportsSkipLocked()) {
                """
                SELECT id
                FROM toss_billing_subscriptions
                WHERE status = :status
                  AND (
                    (retry_at IS NOT NULL AND retry_at <= :now)
                    OR (retry_at IS NULL AND next_billing_at IS NOT NULL AND next_billing_at <= :now)
                  )
                ORDER BY id
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
                """.trimIndent()
            } else {
                """
                SELECT id
                FROM toss_billing_subscriptions
                WHERE status = :status
                  AND (
                    (retry_at IS NOT NULL AND retry_at <= :now)
                    OR (retry_at IS NULL AND next_billing_at IS NOT NULL AND next_billing_at <= :now)
                  )
                ORDER BY id
                LIMIT :limit
                FOR UPDATE
                """.trimIndent()
            }

        @Suppress("UNCHECKED_CAST")
        val rows =
            entityManager
                .createNativeQuery(sql)
                .setParameter("status", TossBillingSubscriptionStatus.ACTIVE.name)
                .setParameter("now", now)
                .setParameter("limit", limit)
                .resultList as List<Any>

        return rows.map { row ->
            when (row) {
                is Number -> row.toLong()
                is Array<*> -> (row[0] as Number).toLong()
                else -> (row as Number).toLong()
            }
        }
    }

    private fun supportsSkipLocked(): Boolean {
        val dialect =
            entityManager
                .unwrap(Session::class.java)
                .sessionFactory.properties["hibernate.dialect"]
                ?.toString()
                .orEmpty()
                .lowercase()
        return dialect.contains("postgres") || dialect.contains("mysql") || dialect.contains("mariadb")
    }
}
