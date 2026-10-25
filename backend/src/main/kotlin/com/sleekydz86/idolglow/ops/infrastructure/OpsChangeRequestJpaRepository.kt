package com.sleekydz86.idolglow.ops.infrastructure

import com.sleekydz86.idolglow.ops.domain.OpsChangeRequest
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface OpsChangeRequestJpaRepository : JpaRepository<OpsChangeRequest, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from OpsChangeRequest request where request.id = :id")
    fun findByIdForUpdate(id: Long): OpsChangeRequest?

    fun findTop50ByOrderByIdDesc(): List<OpsChangeRequest>
}
