package com.sleekydz86.idolglow.payment.infrastructure

import com.sleekydz86.idolglow.payment.domain.TossBillingKeyDeletionStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface TossBillingMethodJpaRepository : JpaRepository<TossBillingMethod, Long> {
    fun findByUserIdAndActiveTrueAndIsDefaultTrue(userId: Long): TossBillingMethod?

    fun findAllByUserIdAndActiveTrueOrderByIsDefaultDescIdDesc(userId: Long): List<TossBillingMethod>

    fun findAllByUserIdOrderByIdDesc(userId: Long): List<TossBillingMethod>

    fun findByIdAndUserIdAndActiveTrue(
        id: Long,
        userId: Long,
    ): TossBillingMethod?

    fun findByBillingKeyHash(billingKeyHash: String): TossBillingMethod?

    fun countByUserId(userId: Long): Long

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from TossBillingMethod m where m.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): TossBillingMethod?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from TossBillingMethod m where m.id = :id and m.userId = :userId")
    fun findByIdAndUserIdForUpdate(
        @Param("id") id: Long,
        @Param("userId") userId: Long,
    ): TossBillingMethod?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from TossBillingMethod m where m.billingKeyHash = :hash")
    fun findByBillingKeyHashForUpdate(
        @Param("hash") hash: String,
    ): TossBillingMethod?

    @Query(
        """
        select m.id from TossBillingMethod m
        where m.deletionStatus = :pending
          and m.deletionRequestedAt is not null
          and (
            (m.deletionLastAttemptAt is not null and m.deletionLastAttemptAt <= :attemptedBefore)
            or (m.deletionLastAttemptAt is null and m.deletionRequestedAt <= :attemptedBefore)
          )
        order by m.id asc
        """,
    )
    fun findDeletionRetryIds(
        @Param("pending") pending: TossBillingKeyDeletionStatus,
        @Param("attemptedBefore") attemptedBefore: LocalDateTime,
        pageable: Pageable,
    ): List<Long>
}
