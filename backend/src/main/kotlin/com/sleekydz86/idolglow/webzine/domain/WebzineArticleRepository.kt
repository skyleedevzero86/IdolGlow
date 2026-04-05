package com.sleekydz86.idolglow.webzine.domain

interface WebzineArticleRepository {
    fun findByIssueIdAndSlug(issueId: Long, slug: String): WebzineArticle?
    fun save(article: WebzineArticle): WebzineArticle
    fun delete(article: WebzineArticle)
}
