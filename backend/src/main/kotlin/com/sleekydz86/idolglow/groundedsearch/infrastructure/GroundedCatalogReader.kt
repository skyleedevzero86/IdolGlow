package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.application.port.GroundedCatalogPort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedFilter
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedOptionPrice
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedProductFacts
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSlotFact
import com.sleekydz86.idolglow.productpackage.product.domain.ProductOption
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class GroundedCatalogReader(
    entityManager: EntityManager,
) : GroundedCatalogPort {
    private val query = GroundedCatalogQuery(entityManager)

    @Transactional(readOnly = true)
    override fun match(filter: GroundedFilter): Set<Long> = narrow(filter)

    @Transactional(readOnly = true)
    override fun optionOwners(productIds: Set<Long>): Map<Long, Long> = query.optionOwners(productIds)

    @Transactional(readOnly = true)
    override fun facts(productIds: Set<Long>, dateFrom: LocalDate?, dateTo: LocalDate?): List<GroundedProductFacts> {
        if (productIds.isEmpty()) {
            return emptyList()
        }
        val options = query.options(productIds).groupBy { it.product.id }
        val slots = query.slots(productIds, dateFrom, dateTo).groupBy { it.product.id }
        return query.products(productIds).map { product ->
            fact(product.id, product.name, product.basePrice, options, slots)
        }
    }

    private fun narrow(filter: GroundedFilter): Set<Long> {
        var ids = query.allIds()
        if (filter.region != null) {
            ids = ids.intersect(query.regionIds(filter.region))
        }
        if (filter.category != null) {
            ids = ids.intersect(query.categoryIds(filter.category))
        }
        if (filter.maxBudget != null) {
            ids = ids.intersect(query.budgetIds(filter.maxBudget))
        }
        if (filter.dateFrom != null && filter.dateTo != null) {
            ids = ids.intersect(query.slotProductIds(ids, filter.dateFrom, filter.dateTo))
        }
        return ids
    }

    private fun fact(
        productId: Long,
        name: String,
        basePrice: BigDecimal,
        options: Map<Long, List<ProductOption>>,
        slots: Map<Long, List<ReservationSlot>>,
    ): GroundedProductFacts {
        val priced =
            options[productId].orEmpty().map { option ->
                GroundedOptionPrice(option.option.id, option.option.name, option.option.price)
            }
        val prices = priced.map { it.price }
        val min = if (prices.isEmpty()) basePrice else basePrice + prices.min()
        val times = slots[productId].orEmpty().map { GroundedSlotFact(it.reservationDate, it.startTime, it.endTime) }
        return GroundedProductFacts(productId, name, min, priced, times)
    }
}
