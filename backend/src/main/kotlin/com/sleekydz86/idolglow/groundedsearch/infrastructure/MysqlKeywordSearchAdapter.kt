package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.application.port.KeywordSearchPort
import jakarta.persistence.EntityManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@ConditionalOnProperty(name = ["spring.datasource.driver-class-name"], havingValue = "com.mysql.cj.jdbc.Driver")
class MysqlKeywordSearchAdapter(
    private val entityManager: EntityManager,
) : KeywordSearchPort {
    @Transactional(readOnly = true)
    override fun search(query: String, limit: Int): List<Long> {
        val trimmed = booleanQuery(query)
        if (trimmed.isEmpty()) {
            return emptyList()
        }
        val rows =
            entityManager
                .createNativeQuery(SQL)
                .setParameter("q", trimmed)
                .setMaxResults(limit)
                .resultList
        return rows.map { (it as Number).toLong() }
    }

    private fun booleanQuery(query: String): String = query.filterNot { it in BOOLEAN_MARKS }.trim()

    private companion object {
        private const val BOOLEAN_MARKS = "+-<>()~*\"@"
        private val SQL =
            """
            select id
            from rag_chunks
            where match(content) against (:q in boolean mode)
            order by match(content) against (:q in boolean mode) desc
            """.trimIndent()
    }
}
