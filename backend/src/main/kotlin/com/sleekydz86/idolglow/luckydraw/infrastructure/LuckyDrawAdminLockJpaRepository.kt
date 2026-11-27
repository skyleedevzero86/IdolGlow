package com.sleekydz86.idolglow.luckydraw.infrastructure

import com.sleekydz86.idolglow.luckydraw.domain.LuckyDrawAdminLock
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LuckyDrawAdminLockJpaRepository : JpaRepository<LuckyDrawAdminLock, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LuckyDrawAdminLock l where l.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): LuckyDrawAdminLock?
}
