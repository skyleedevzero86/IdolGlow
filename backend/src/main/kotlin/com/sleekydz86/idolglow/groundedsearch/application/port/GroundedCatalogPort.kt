package com.sleekydz86.idolglow.groundedsearch.application.port

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedFilter
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedProductFacts
import java.time.LocalDate

interface GroundedCatalogPort {
    fun match(filter: GroundedFilter): Set<Long>

    fun optionOwners(productIds: Set<Long>): Map<Long, Long>

    fun facts(productIds: Set<Long>, dateFrom: LocalDate?, dateTo: LocalDate?): List<GroundedProductFacts>
}
