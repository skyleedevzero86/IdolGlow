package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductOption
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationSlot
import jakarta.persistence.EntityManager
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class GroundedCatalogQuery(
    entityManager: EntityManager,
) {
    private val jpql = GroundedJpql(entityManager)
    private val budget = GroundedBudgetMatch(jpql)

    fun allIds(): Set<Long> = jpql.longIds("select p.id from Product p")

    fun regionIds(region: String): Set<Long> = jpql.longIds(REGION, mapOf("region" to jpql.like(region)))

    fun categoryIds(category: String): Set<Long> =
        jpql.longIds(CATEGORY, mapOf("category" to jpql.like(category.lowercase())))

    fun budgetIds(maxBudget: BigDecimal): Set<Long> = budget.ids(maxBudget)

    fun slotProductIds(ids: Set<Long>, from: LocalDate, to: LocalDate): Set<Long> {
        if (ids.isEmpty()) {
            return emptySet()
        }
        return jpql.longIds(SLOT_IDS, mapOf("ids" to ids, "start" to from, "end" to to, "now" to LocalDateTime.now()))
    }

    fun optionOwners(ids: Set<Long>): Map<Long, Long> {
        if (ids.isEmpty()) {
            return emptyMap()
        }
        return jpql.pairs(OWNERS, mapOf("ids" to ids)).associate { it.first to it.second.toLong() }
    }

    fun products(ids: Set<Long>): List<Product> = listed("select p from Product p where p.id in :ids", ids)

    fun options(ids: Set<Long>): List<ProductOption> =
        listed("select po from ProductOption po join fetch po.option where po.product.id in :ids", ids)

    fun slots(ids: Set<Long>, from: LocalDate?, to: LocalDate?): List<ReservationSlot> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        val params = mutableMapOf<String, Any>("ids" to ids, "now" to LocalDateTime.now())
        if (from != null && to != null) {
            params["start"] = from
            params["end"] = to
        }
        val sql = if (from != null && to != null) SLOT_DATED else SLOT_OPEN
        return jpql.listed(sql, params)
    }

    private fun <T> listed(sql: String, ids: Set<Long>): List<T> =
        if (ids.isEmpty()) emptyList() else jpql.listed(sql, mapOf("ids" to ids))

    private companion object {
        private const val AVAILABLE =
            "rs.isStatusBooked = false and (rs.holdExpiresAt is null or rs.holdExpiresAt < :now)"
        private const val REGION =
            """
            select distinct p.id from Product p
            left join p.productLocation pl
            left join p.productOptions po
            left join po.option o
            where pl.name like :region
            or pl.roadAddressName like :region
            or pl.addressName like :region
            or o.location like :region
            """
        private const val CATEGORY =
            """
            select distinct p.id from Product p
            left join p.productTags tag
            where lower(p.name) like :category
            or lower(p.description) like :category
            or lower(tag.tagName) like :category
            """
        private const val SLOT_IDS =
            "select distinct rs.product.id from ReservationSlot rs where rs.product.id in :ids and rs.reservationDate between :start and :end and $AVAILABLE"
        private const val OWNERS =
            "select o.id, po.product.id from ProductOption po join po.option o where po.product.id in :ids"
        private const val SLOT_OPEN =
            "select rs from ReservationSlot rs where rs.product.id in :ids and $AVAILABLE"
        private const val SLOT_DATED =
            "select rs from ReservationSlot rs where rs.product.id in :ids and rs.reservationDate between :start and :end and $AVAILABLE"
    }
}
