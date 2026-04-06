package com.sleekydz86.idolglow.newsletter.domain

interface NewsletterRepository {
    fun findAllByLatest(): List<Newsletter>
    fun findBySlug(slug: String): Newsletter?
    fun existsBySlug(slug: String): Boolean
    fun existsBySlugAndIdNot(slug: String, id: Long): Boolean
    fun save(newsletter: Newsletter): Newsletter
    fun delete(newsletter: Newsletter)
    fun count(): Long
}
