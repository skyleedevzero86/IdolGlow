package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.application.port.KeywordSearchPort
import jakarta.persistence.EntityManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@ConditionalOnProperty(name = ["spring.datasource.driver-class-name"], havingValue = "org.h2.Driver")
class LikeKeywordSearchAdapter(
    private val entityManager: EntityManager,
) : KeywordSearchPort {
    @Transactional(readOnly = true)
    override fun search(query: String, limit: Int): List<Long> {
        val trimmed = query.replace("%", "").replace("_", "").trim()
        if (trimmed.isEmpty()) {
            return emptyList()
        }
        return entityManager
            .createQuery(
                "select c.id from RagChunk c where lower(c.content) like lower(concat('%', :q, '%'))",
                Long::class.java,
            ).setParameter("q", trimmed)
            .setMaxResults(limit)
            .resultList
    }
}
