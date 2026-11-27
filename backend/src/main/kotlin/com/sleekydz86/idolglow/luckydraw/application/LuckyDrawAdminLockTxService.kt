package com.sleekydz86.idolglow.luckydraw.application

import com.sleekydz86.idolglow.luckydraw.domain.LuckyDrawAdminLock
import com.sleekydz86.idolglow.luckydraw.infrastructure.LuckyDrawAdminLockJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class LuckyDrawAdminLockTxService(
    private val luckyDrawAdminLockJpaRepository: LuckyDrawAdminLockJpaRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun ensureLockRow() {
        if (luckyDrawAdminLockJpaRepository.existsById(LuckyDrawAdminLock.SINGLE_ID)) return
        luckyDrawAdminLockJpaRepository.saveAndFlush(LuckyDrawAdminLock())
    }
}
