package com.sleekydz86.idolglow.newsletter.infrastructure

import com.sleekydz86.idolglow.newsletter.domain.Newsletter
import org.springframework.data.jpa.repository.JpaRepository

interface NewsletterJpaRepository : JpaRepository<Newsletter, Long> {
    fun findBySlug(slug: String): Newsletter?
    fun existsBySlug(slug: String): Boolean
    fun existsBySlugAndIdNot(slug: String, id: Long): Boolean
}
