package com.sleekydz86.idolglow.idolglowissue.domain

interface IdolGlowArticleRepository {
    fun findByIssueIdAndSlug(
        issueId: Long,
        slug: String,
    ): IdolGlowArticle?

    fun save(article: IdolGlowArticle): IdolGlowArticle

    fun delete(article: IdolGlowArticle)
}
