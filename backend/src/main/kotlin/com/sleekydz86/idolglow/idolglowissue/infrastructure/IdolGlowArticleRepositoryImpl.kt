package com.sleekydz86.idolglow.idolglowissue.infrastructure

import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowArticle
import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowArticleRepository
import org.springframework.stereotype.Repository

@Repository
class IdolGlowArticleRepositoryImpl(
    private val idolglowArticleJpaRepository: IdolGlowArticleJpaRepository,
) : IdolGlowArticleRepository {
    override fun findByIssueIdAndSlug(
        issueId: Long,
        slug: String,
    ): IdolGlowArticle? = idolglowArticleJpaRepository.findByIssue_IdAndSlug(issueId, slug)

    override fun save(article: IdolGlowArticle): IdolGlowArticle = idolglowArticleJpaRepository.save(article)

    override fun delete(article: IdolGlowArticle) {
        idolglowArticleJpaRepository.delete(article)
    }
}
