package com.sleekydz86.idolglow.newsletter.infrastructure

import com.sleekydz86.idolglow.newsletter.domain.Newsletter
import com.sleekydz86.idolglow.newsletter.domain.NewsletterRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class NewsletterRepositoryImpl(
    private val newsletterJpaRepository: NewsletterJpaRepository,
) : NewsletterRepository {

    override fun findAllByLatest(): List<Newsletter> =
        newsletterJpaRepository.findAll(
            Sort.by(
                Sort.Order.desc("publishedAt"),
                Sort.Order.desc("createdAt"),
            )
        )

    override fun findBySlug(slug: String): Newsletter? =
        newsletterJpaRepository.findBySlug(slug)

    override fun existsBySlug(slug: String): Boolean =
        newsletterJpaRepository.existsBySlug(slug)

    override fun existsBySlugAndIdNot(slug: String, id: Long): Boolean =
        newsletterJpaRepository.existsBySlugAndIdNot(slug, id)

    override fun save(newsletter: Newsletter): Newsletter =
        newsletterJpaRepository.save(newsletter)

    override fun delete(newsletter: Newsletter) {
        newsletterJpaRepository.delete(newsletter)
    }

    override fun count(): Long =
        newsletterJpaRepository.count()
}
