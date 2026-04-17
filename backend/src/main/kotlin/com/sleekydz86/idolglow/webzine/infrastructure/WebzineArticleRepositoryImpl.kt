package com.sleekydz86.idolglow.webzine.infrastructure

import com.sleekydz86.idolglow.webzine.domain.WebzineArticle
import com.sleekydz86.idolglow.webzine.domain.WebzineArticleRepository
import org.springframework.stereotype.Repository

@Repository
class WebzineArticleRepositoryImpl(
    private val webzineArticleJpaRepository: WebzineArticleJpaRepository,
) : WebzineArticleRepository {

    override fun findByIssueIdAndSlug(issueId: Long, slug: String): WebzineArticle? =
        webzineArticleJpaRepository.findByIssue_IdAndSlug(issueId, slug)

    override fun save(article: WebzineArticle): WebzineArticle =
        webzineArticleJpaRepository.save(article)

    override fun delete(article: WebzineArticle) {
        webzineArticleJpaRepository.delete(article)
    }
}
