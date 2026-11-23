package com.sleekydz86.idolglow.groundedsearch.infrastructure

import jakarta.persistence.EntityManager
import java.math.BigDecimal

class GroundedJpql(
    private val entityManager: EntityManager,
) {
    fun longIds(jpql: String, params: Map<String, Any> = emptyMap()): Set<Long> {
        @Suppress("UNCHECKED_CAST")
        val rows = query(jpql, params).resultList as List<Any>
        return rows.map { (it as Number).toLong() }.toSet()
    }

    fun pairs(jpql: String, params: Map<String, Any> = emptyMap()): List<Pair<Long, BigDecimal>> {
        @Suppress("UNCHECKED_CAST")
        val rows = query(jpql, params).resultList as List<Array<Any>>
        return rows.map { (it[0] as Number).toLong() to decimal(it[1]) }
    }

    fun <T> listed(jpql: String, params: Map<String, Any>): List<T> {
        @Suppress("UNCHECKED_CAST")
        return query(jpql, params).resultList as List<T>
    }

    fun like(value: String): String = "%${value.replace("%", "").replace("_", "")}%"

    private fun query(jpql: String, params: Map<String, Any>) =
        entityManager.createQuery(jpql).also { query ->
            params.forEach { (name, value) -> query.setParameter(name, value) }
        }

    private fun decimal(value: Any): BigDecimal = if (value is BigDecimal) value else BigDecimal(value.toString())
}
