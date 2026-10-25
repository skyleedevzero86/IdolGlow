package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.application.port.KeywordSearchPort
import jakarta.persistence.EntityManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@ConditionalOnProperty(name = ["spring.datasource.driver-class-name"], havingValue = "org.postgresql.Driver")
class PostgresKeywordSearchAdapter(
    private val entityManager: EntityManager,
) : KeywordSearchPort {
    @Transactional(readOnly = true)
    override fun search(query: String, limit: Int): List<Long> {
        val trimmed = query.trim()
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

    private companion object {
        private val SQL =
            """
            select id
            from rag_chunks
            where word_similarity(:q, content) > 0
            order by word_similarity(:q, content) desc
            """.trimIndent()
    }
}
