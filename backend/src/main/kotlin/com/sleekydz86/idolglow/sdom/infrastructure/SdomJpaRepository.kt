package com.sleekydz86.idolglow.sdom.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface SdomJpaRepository : JpaRepository<SdomEntity, String> {
    fun findByUseAtOrderBySortSeqAsc(useAt: String): List<SdomEntity>
}
