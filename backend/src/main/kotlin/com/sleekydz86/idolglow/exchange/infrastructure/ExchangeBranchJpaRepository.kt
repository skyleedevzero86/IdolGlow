package com.sleekydz86.idolglow.exchange.infrastructure

import com.sleekydz86.idolglow.exchange.domain.ExchangeBranch
import org.springframework.data.jpa.repository.JpaRepository

interface ExchangeBranchJpaRepository : JpaRepository<ExchangeBranch, Long> {
    fun findByCurrencyOrderBySortOrderAsc(currency: String): List<ExchangeBranch>
}
