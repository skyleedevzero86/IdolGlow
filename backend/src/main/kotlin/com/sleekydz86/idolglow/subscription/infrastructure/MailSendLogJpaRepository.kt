package com.sleekydz86.idolglow.subscription.infrastructure

import com.sleekydz86.idolglow.subscription.domain.MailSendLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface MailSendLogJpaRepository :
    JpaRepository<MailSendLog, Long>,
    JpaSpecificationExecutor<MailSendLog> {
    fun findFirstByDispatchIdOrderByIdDesc(dispatchId: Long): MailSendLog?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from MailSendLog l where l.createdAt < :before")
    fun deleteCreatedBefore(
        @Param("before") before: LocalDateTime,
    ): Int
}
